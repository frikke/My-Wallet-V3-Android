package com.blockchain.coincore.impl.txEngine

import androidx.annotation.VisibleForTesting
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.coincore.xlm.STATE_MEMO
import com.blockchain.core.TransactionsStore
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.then
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

private fun PendingTx.setMemo(memo: TxConfirmationValue.Memo): PendingTx =
    this.copy(
        engineState = engineState.copyAndPut(STATE_MEMO, memo)
    )

private val PendingTx.memo: String?
    get() {
        val memo = (this.engineState[STATE_MEMO] as? TxConfirmationValue.Memo)
        return memo?.let {
            return memo.text ?: memo.id.toString()
        }
    }

// Transfer from a custodial trading account to an onChain non-custodial account
class TradingToOnChainTxEngine(
    private val tradingStore: TradingStore,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val isNoteSupported: Boolean = false,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    private val userIdentity: UserIdentity,
    private val limitsDataManager: LimitsDataManager
) : TxEngine() {

    private val transactionsStore: TransactionsStore by scopedInject()

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(tradingStore, transactionsStore)

    override fun ensureSourceBalanceFreshness() {
        tradingStore.markAsStale()
    }

    override fun assertInputsValid() {
        check(txTarget is CryptoAddress)
        check(sourceAsset == (txTarget as CryptoAddress).asset)
        check(sourceAccount is SingleAccount)
    }

    private val sourceAssetInfo: AssetInfo
        get() = sourceAsset.asAssetInfoOrThrow()

    override fun doInitialiseTx(): Single<PendingTx> {
        val withdrawFeeAndMinLimit =
            walletManager.fetchCryptoWithdrawFeeAndMinLimit(sourceAssetInfo, Product.BUY).cache()
        return Single.zip(
            sourceAccount.balanceRx().firstOrError(),
            withdrawFeeAndMinLimit,
            limitsDataManager.getLimits(
                outputCurrency = sourceAsset,
                sourceCurrency = sourceAsset,
                targetCurrency = (txTarget as CryptoAddress).asset,
                sourceAccountType = AssetCategory.CUSTODIAL,
                targetAccountType = AssetCategory.NON_CUSTODIAL,
                legacyLimits = withdrawFeeAndMinLimit.map { limits ->
                    object : LegacyLimits {
                        override val min: Money
                            get() = Money.fromMinor(sourceAsset, limits.minLimit)
                        override val max: Money?
                            get() = null
                    }
                }
            )
        ) { balance, cryptoFee, limits ->
            val fees = Money.fromMinor(sourceAsset, cryptoFee.fee)
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = balance.total,
                availableBalance = Money.max(balance.withdrawable - fees, Money.zero(sourceAsset)),
                feeForFullAvailable = fees,
                feeAmount = fees,
                feeSelection = FeeSelection(),
                selectedFiat = userFiat,
                limits = limits
            )
        }
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return Single.zip(
            sourceAccount.balanceRx().firstOrError(),
            walletManager.fetchCryptoWithdrawFeeAndMinLimit(sourceAssetInfo, Product.BUY)
        ) { balance, cryptoFeeAndMin ->
            val fees = Money.fromMinor(sourceAsset, cryptoFeeAndMin.fee)
            pendingTx.copy(
                amount = amount,
                totalBalance = balance.total,
                availableBalance = Money.max(balance.withdrawable - fees, Money.zero(sourceAsset))
            )
        }
    }

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget, AssetAction.Send, sourceAccount
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        receivingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toUserFiat(exchangeRates),
                                sourceAsset
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel,
                        ignoreErc20LinkedNote = true
                    ),
                    TxConfirmationValue.Total(
                        totalWithFee = (pendingTx.amount as CryptoValue).plus(
                            pendingTx.feeAmount as CryptoValue
                        ),
                        exchange = pendingTx.amount.toUserFiat(exchangeRates)
                            .plus(pendingTx.feeAmount.toUserFiat(exchangeRates))
                    ),
                    if (isNoteSupported) {
                        TxConfirmationValue.Description()
                    } else null,
                    if ((sourceAccount as SingleAccount).isMemoSupported) {
                        val memo = (txTarget as? CryptoAddress)?.memo
                        TxConfirmationValue.Memo(
                            text = memo,
                            id = null
                        )
                    } else null
                )
            )
        )

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> {
        return super.doOptionUpdateRequest(pendingTx, newConfirmation)
            .map { tx ->
                (newConfirmation as? TxConfirmationValue.Memo)?.let {
                    tx.setMemo(newConfirmation)
                } ?: tx
            }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).then { validateOptions(pendingTx) }.updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.defer {
            when {
                pendingTx.isMinLimitViolated() -> Completable.error(
                    TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
                )
                pendingTx.isMaxLimitViolated() -> aboveTierLimit()
                pendingTx.amount > pendingTx.availableBalance -> Completable.error(
                    TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
                )
                else -> Completable.complete()
            }
        }

    private fun aboveTierLimit(): Completable {
        return userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD)).onErrorReturnItem(false)
            .flatMapCompletable { gold ->
                if (gold) {
                    Completable.error(
                        TxValidationFailure(ValidationState.OVER_GOLD_TIER_LIMIT)
                    )
                } else {
                    Completable.error(
                        TxValidationFailure(ValidationState.OVER_SILVER_TIER_LIMIT)
                    )
                }
            }
    }

    private fun isMemoValid(memoConfirmation: String?): Boolean {
        return if (memoConfirmation.isNullOrBlank()) {
            true
        } else {
            when (sourceAssetInfo.networkTicker) {
                "XLM" -> memoConfirmation.length in 1..28
                "STX" -> memoConfirmation.length in 1..34
                else -> memoConfirmation.length in 1..28
            }
        }
    }

    private fun getMemoOption(pendingTx: PendingTx) =
        pendingTx.memo

    private fun validateOptions(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (!isMemoValid(getMemoOption(pendingTx))) {
                throw TxValidationFailure(ValidationState.OPTION_INVALID)
            }
        }

    // The custodial balance now returns an id, so it is possible to add a note via this
    // processor at some point.
    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        val targetAddress = txTarget as CryptoAddress
        val address = pendingTx.memo?.takeIf { it.isNotEmpty() }?.let {
            "${targetAddress.address}:$it"
        } ?: targetAddress.address

        return walletManager.transferFundsToWallet(
            amount = pendingTx.amount as CryptoValue,
            fee = pendingTx.feeAmount as CryptoValue,
            walletAddress = address
        ).updateTxValidation()
            .map {
                TxResult.UnHashedTxResult(pendingTx.amount)
            }
    }
}

private fun Single<String>.updateTxValidation(): Single<String> {
    return this.onErrorResumeNext {
        val nabuException = it as? NabuApiException ?: return@onErrorResumeNext Single.error(it)
        return@onErrorResumeNext when (nabuException.getErrorStatusCode()) {
            NabuErrorStatusCodes.Forbidden -> {
                if (nabuException.getErrorCode() == NabuErrorCodes.WithdrawLocked) {
                    Single.error(TransactionError.WithdrawalBalanceLocked)
                } else {
                    Single.error(TransactionError.WithdrawalAlreadyPending)
                }
            }
            NabuErrorStatusCodes.Conflict -> Single.error(
                TransactionError.WithdrawalInsufficientFunds
            )
            else -> Single.error(nabuException)
        }
    }
}
