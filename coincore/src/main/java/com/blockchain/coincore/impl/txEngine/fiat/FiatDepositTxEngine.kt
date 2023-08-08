package com.blockchain.coincore.impl.txEngine.fiat

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NeedsApprovalException
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.txEngine.MissingLimitsException
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.TransactionsStore
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.NON_CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankPartnerCallbackProvider
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.BankTransferAction
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.domain.paymentmethods.model.SettlementType
import com.blockchain.extensions.filterNotNullValues
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.network.PollService
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.secondsToDays
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.security.InvalidParameterException

const val WITHDRAW_LOCKS = "locks"
private const val PAYMENT_METHOD_LIMITS = "PAYMENT_METHOD_LIMITS"

class FiatDepositTxEngine(
    private val walletManager: CustodialWalletManager,
    private val bankService: BankService,
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val limitsDataManager: LimitsDataManager,
    private val userIdentity: UserIdentity,
    private val withdrawLocksRepository: WithdrawLocksRepository,
    private val plaidFeatureFlag: FeatureFlag
) : TxEngine() {

    private val transactionsStore: TransactionsStore by scopedInject()
    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(transactionsStore)

    override fun ensureSourceBalanceFreshness() {}

    private val userIsGoldVerified: Single<Boolean>
        get() = userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD))

    override fun assertInputsValid() {
        check(sourceAccount is BankAccount)
        check(txTarget is FiatAccount)
    }

    override fun doInitialiseTx(): Single<PendingTx> {
        check(sourceAccount is BankAccount)
        check(txTarget is FiatAccount)
        val sourceAccountCurrency = (sourceAccount as LinkedBankAccount).currency
        val zeroFiat = Money.zero(sourceAccountCurrency)
        val paymentMethodLimits = walletManager.getBankTransferLimits(sourceAccountCurrency, true)
        val locks = withdrawLocksRepository.getWithdrawLockTypeForPaymentMethod(
            paymentMethodType = PaymentMethodType.BANK_TRANSFER,
            fiatCurrency = sourceAccountCurrency
        )

        return paymentMethodLimits.zipWith(locks)
            .flatMap { (paymentMethodLimits, locks) ->
                limitsDataManager.getLimits(
                    outputCurrency = sourceAccountCurrency,
                    sourceCurrency = sourceAccountCurrency,
                    targetCurrency = (txTarget as FiatAccount).currency,
                    sourceAccountType = NON_CUSTODIAL_LIMITS_ACCOUNT,
                    targetAccountType = CUSTODIAL_LIMITS_ACCOUNT,
                    legacyLimits = Single.just(paymentMethodLimits).map {
                        it
                    }
                ).map { limits ->
                    PendingTx(
                        amount = zeroFiat,
                        totalBalance = zeroFiat,
                        availableBalance = zeroFiat,
                        feeForFullAvailable = zeroFiat,
                        limits = limits,
                        feeAmount = zeroFiat,
                        selectedFiat = userFiat,
                        feeSelection = FeeSelection(),
                        engineState = mapOf(
                            WITHDRAW_LOCKS to locks.takeIf { it.signum() == 1 }?.secondsToDays(),
                            PAYMENT_METHOD_LIMITS to TxLimits.fromAmounts(
                                paymentMethodLimits.min,
                                paymentMethodLimits.max
                            )
                        ).filterNotNullValues()
                    )
                }
            }
    }

    override val canTransactFiat: Boolean
        get() = true

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                amount = amount
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.PaymentMethod(
                        sourceAccount.label,
                        (sourceAccount as LinkedBankAccount).accountNumber,
                        (sourceAccount as LinkedBankAccount).accountType,
                        AssetAction.FiatDeposit
                    ),
                    TxConfirmationValue.To(txTarget, AssetAction.FiatDeposit),
                    if (!isOpenBankingCurrency()) {
                        TxConfirmationValue.EstimatedCompletion
                    } else {
                        null
                    },
                    TxConfirmationValue.Amount(pendingTx.amount, true)
                )
            )
        )
    }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        return if (pendingTx.validationState == ValidationState.UNINITIALISED && pendingTx.amount.isZero) {
            Single.just(pendingTx)
        } else {
            validateAmount(pendingTx).updateTxValidity(pendingTx)
        }
    }

    private fun validateAmount(pendingTx: PendingTx): Completable =
        Completable.defer {
            if (pendingTx.limits != null) {
                when {
                    pendingTx.amount.isZero -> Completable.error(TxValidationFailure(ValidationState.INVALID_AMOUNT))
                    pendingTx.isMinLimitViolated() -> Completable.error(
                        TxValidationFailure(
                            ValidationState.UNDER_MIN_LIMIT
                        )
                    )
                    pendingTx.maxLimitForPaymentMethodViolated() ->
                        Completable.error(
                            TxValidationFailure(ValidationState.ABOVE_PAYMENT_METHOD_LIMIT)
                        )
                    pendingTx.isMaxLimitViolated() -> {
                        userIsGoldVerified.flatMapCompletable {
                            if (it) {
                                Completable.error(TxValidationFailure(ValidationState.OVER_GOLD_TIER_LIMIT))
                            } else {
                                Completable.error(TxValidationFailure(ValidationState.OVER_SILVER_TIER_LIMIT))
                            }
                        }
                    }
                    else -> Completable.complete()
                }
            } else {
                Completable.error(
                    MissingLimitsException(
                        AssetAction.FiatDeposit,
                        sourceAccount,
                        txTarget
                    )
                )
            }
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        sourceAccount.receiveAddress.flatMap {
            plaidFeatureFlag.enabled.flatMap { isPlaidEnabled ->
                if (isPlaidEnabled) {
                    checkSettlementBeforeDeposit(it, pendingTx)
                } else {
                    startBankTransfer(it, pendingTx)
                }
            }
        }.map {
            TxResult.HashedTxResult(it, pendingTx.amount)
        }

    private fun checkSettlementBeforeDeposit(it: ReceiveAddress, pendingTx: PendingTx) =
        bankService.checkSettlement(it.address, pendingTx.amount)
            .zipWith(bankService.getLinkedBankLegacy(it.address))
            .flatMap { (settlement, linkedBank) ->
                val isYodleeUpgradeRequired = linkedBank.partner == BankPartner.YODLEE &&
                    settlement.settlementReason == SettlementReason.REQUIRES_UPDATE

                if (settlement.settlementType == SettlementType.UNAVAILABLE || isYodleeUpgradeRequired) {
                    when (settlement.settlementReason) {
                        SettlementReason.GENERIC,
                        SettlementReason.UNKNOWN ->
                            Single.error(TransactionError.SettlementGenericError)
                        SettlementReason.INSUFFICIENT_BALANCE ->
                            Single.error(TransactionError.SettlementInsufficientBalance)
                        SettlementReason.STALE_BALANCE ->
                            Single.error(TransactionError.SettlementStaleBalance)
                        SettlementReason.REQUIRES_UPDATE ->
                            Single.error(TransactionError.SettlementRefreshRequired(it.address))
                        SettlementReason.NONE ->
                            startBankTransfer(it, pendingTx)
                    }
                } else {
                    startBankTransfer(it, pendingTx)
                }
            }

    private fun startBankTransfer(receiveAddress: ReceiveAddress, pendingTx: PendingTx) =
        bankService.startBankTransfer(
            id = receiveAddress.address,
            amount = pendingTx.amount,
            currency = pendingTx.amount.currencyCode,
            callback = if (isOpenBankingCurrency()) {
                bankPartnerCallbackProvider.callback(
                    BankPartner.YAPILY,
                    BankTransferAction.PAY
                )
            } else {
                null
            }
        )

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable {
        return plaidFeatureFlag.enabled.flatMapCompletable { isPlaidEnabled ->
            if (isOpenBankingCurrency()) {
                pollForOpenBanking(txResult)
            } else if (isPlaidEnabled) {
                pollForPlaid(txResult)
            } else {
                Completable.complete()
            }
        }
    }

    private fun pollForOpenBanking(txResult: TxResult): Completable {
        val paymentId = (txResult as TxResult.HashedTxResult).txId
        return PollService(bankService.getBankTransferCharge(paymentId)) {
            it.authorisationUrl != null || it.status is BankTransferStatus.Error
        }.start()
            .map { it.value }
            .flatMapCompletable { bankTransferDetails ->
                (bankTransferDetails.status as? BankTransferStatus.Error)?.error?.let { errorCode ->
                    Completable.error(TransactionError.FiatDepositError(errorCode))
                } ?: run {
                    bankService.getLinkedBankLegacy(bankTransferDetails.id).map { linkedBank ->
                        bankTransferDetails.authorisationUrl?.let {
                            BankPaymentApproval(
                                paymentId,
                                it,
                                linkedBank,
                                bankTransferDetails.amount as FiatValue
                            )
                        } ?: throw InvalidParameterException("No auth url was returned")
                    }.flatMapCompletable {
                        Completable.error(NeedsApprovalException(it))
                    }
                }
            }
    }

    private fun pollForPlaid(txResult: TxResult): Completable {
        val paymentId = (txResult as TxResult.HashedTxResult).txId
        return PollService(bankService.getBankTransferCharge(paymentId)) {
            it.status != BankTransferStatus.Pending
        }.start()
            .flatMapCompletable {
                when (it.value.status) {
                    BankTransferStatus.Pending,
                    BankTransferStatus.Unknown,
                    BankTransferStatus.Complete ->
                        Completable.complete()
                    is BankTransferStatus.Error ->
                        (it.value.status as BankTransferStatus.Error).error?.let { error ->
                            Completable.error(TransactionError.FiatDepositError(error))
                        } ?: Completable.complete()
                }
            }
    }

    private fun isOpenBankingCurrency(): Boolean {
        return (sourceAccount as? LinkedBankAccount)?.isOpenBankingCurrency() == true
    }

    private fun PendingTx.maxLimitForPaymentMethodViolated(): Boolean =
        engineState[PAYMENT_METHOD_LIMITS]?.let {
            (it as TxLimits).isAmountOverMax(amount)
        } ?: throw IllegalStateException("Missing Limit for Payment method")
}
