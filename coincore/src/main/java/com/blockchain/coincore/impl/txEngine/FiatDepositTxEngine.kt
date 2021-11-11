package com.blockchain.coincore.impl.txEngine

import androidx.annotation.VisibleForTesting
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.banking.BankTransferAction
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NeedsApprovalException
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.limits.LegacyLimits
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimits
import com.blockchain.extensions.withoutNullValues
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.network.PollService
import com.blockchain.utils.secondsToDays
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.security.InvalidParameterException

const val WITHDRAW_LOCKS = "locks"
private const val PAYMENT_METHOD_LIMITS = "PAYMENT_METHOD_LIMITS"

class FiatDepositTxEngine(
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val limitsDataManager: LimitsDataManager,
    private val userIdentity: UserIdentity,
    private val withdrawLocksRepository: WithdrawLocksRepository
) : TxEngine() {

    private val userIsGoldVerified: Single<Boolean>
        get() = userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD))

    override fun assertInputsValid() {
        check(sourceAccount is BankAccount)
        check(txTarget is FiatAccount)
    }

    override fun doInitialiseTx(): Single<PendingTx> {
        check(sourceAccount is BankAccount)
        check(txTarget is FiatAccount)
        val sourceAccountCurrency = (sourceAccount as LinkedBankAccount).fiatCurrency
        val zeroFiat = FiatValue.zero(sourceAccountCurrency)
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
                    targetCurrency = (txTarget as FiatAccount).fiatCurrency,
                    sourceAccountType = AssetCategory.NON_CUSTODIAL,
                    targetAccountType = AssetCategory.CUSTODIAL,
                    legacyLimits = Single.just(paymentMethodLimits).map {
                        it as LegacyLimits
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
                                paymentMethodLimits.min, paymentMethodLimits.max
                            )
                        ).withoutNullValues()
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
                confirmations = listOfNotNull(
                    TxConfirmationValue.PaymentMethod(
                        sourceAccount.label,
                        (sourceAccount as LinkedBankAccount).accountNumber,
                        (sourceAccount as LinkedBankAccount).accountType,
                        AssetAction.FiatDeposit
                    ),
                    TxConfirmationValue.To(txTarget, AssetAction.FiatDeposit),
                    if (!isOpenBankingCurrency()) {
                        TxConfirmationValue.EstimatedCompletion
                    } else null,
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

                    pendingTx.maxLimitForPaymentMethodViolated() && !pendingTx.isMaxLimitViolated() ->
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
                Completable.error(TxValidationFailure(ValidationState.UNKNOWN_ERROR))
            }
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        sourceAccount.receiveAddress.flatMap {
            walletManager.startBankTransfer(
                it.address, pendingTx.amount, pendingTx.amount.currencyCode,
                if (isOpenBankingCurrency()) {
                    bankPartnerCallbackProvider.callback(BankPartner.YAPILY, BankTransferAction.PAY)
                } else null
            )
        }.map {
            TxResult.HashedTxResult(it, pendingTx.amount)
        }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        if (isOpenBankingCurrency()) {
            val paymentId = (txResult as TxResult.HashedTxResult).txId
            PollService(walletManager.getBankTransferCharge(paymentId)) {
                it.authorisationUrl != null
            }.start().map { it.value }.flatMap { bankTransferDetails ->
                walletManager.getLinkedBank(bankTransferDetails.id).map { linkedBank ->
                    bankTransferDetails.authorisationUrl?.let {
                        BankPaymentApproval(
                            paymentId,
                            it,
                            linkedBank,
                            bankTransferDetails.amount
                        )
                    } ?: throw InvalidParameterException("No auth url was returned")
                }
            }.flatMapCompletable {
                Completable.error(NeedsApprovalException(it))
            }
        } else {
            Completable.complete()
        }

    private fun isOpenBankingCurrency(): Boolean {
        val sourceAccountCurrency = (sourceAccount as LinkedBankAccount).fiatCurrency
        return sourceAccountCurrency == "EUR" || sourceAccountCurrency == "GBP"
    }

    private fun PendingTx.maxLimitForPaymentMethodViolated(): Boolean =
        engineState[PAYMENT_METHOD_LIMITS]?.let {
            (it as TxLimits).isMaxViolatedByAmount(amount)
        } ?: throw IllegalStateException("Missing Limit for Payment method")
}
