package com.blockchain.coincore.selfcustody

import com.blockchain.api.selfcustody.BuildTxResponse
import com.blockchain.api.selfcustody.PushTxResponse
import com.blockchain.api.selfcustody.SignatureAlgorithm
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.coincore.xlm.STATE_MEMO
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.data.FreshnessStrategy
import com.blockchain.extensions.filterNotNullKeys
import com.blockchain.logging.Logger
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.then
import com.blockchain.utils.thenSingle
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
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

class DynamicOnChainTxEngine(
    private val nonCustodialService: NonCustodialService,
    walletPreferences: WalletStatusPrefs,
    requireSecondPassword: Boolean,
    resolvedAddress: Single<String>
) : OnChainTxEngineBase(requireSecondPassword, walletPreferences, resolvedAddress) {

    private val feeCurrency: AssetInfo by lazy {
        nonCustodialService.getFeeCurrencyFor(sourceAsset as AssetInfo)
    }

    private val feeOptions: Single<Map<FeeLevel, CryptoValue>> = rxSingleOutcome {
        nonCustodialService.getFeeOptions(feeCurrency)
            .map {
                it.mapKeys { (level, _) ->
                    level.toPresentation()
                }.filterNotNullKeys()
            }
    }.cache()

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(txTarget, AssetAction.Send, sourceAccount),
                    TxConfirmationValue.CompoundNetworkFee(
                        sendingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toUserFiat(exchangeRates),
                                sourceAsset
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel
                    ),
                    buildConfirmationTotal(pendingTx),
                    if ((sourceAsset as? AssetInfo)?.coinNetwork?.isMemoSupported == true) {
                        val memo = (txTarget as? CryptoAddress)?.memo
                        TxConfirmationValue.Memo(
                            text = memo,
                            id = null
                        )
                    } else null
                )
            )
        )
    }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> {
        return super.doOptionUpdateRequest(pendingTx, newConfirmation)
            .map { tx ->
                (newConfirmation as? TxConfirmationValue.Memo)?.let {
                    tx.setMemo(newConfirmation)
                } ?: tx
            }
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        feeOptions.map { fees ->
            val selectedLevel = when {
                fees.isEmpty() -> FeeLevel.None
                fees.containsKey(FeeLevel.Regular) -> FeeLevel.Regular
                else -> fees.keys.first()
            }
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = fees[selectedLevel] ?: Money.zero(sourceAsset),
                feeAmount = fees[selectedLevel] ?: Money.zero(sourceAsset),
                feeSelection = FeeSelection(
                    selectedLevel = selectedLevel,
                    availableLevels = fees.keys,
                    feesForLevels = fees,
                    asset = feeCurrency,
                ),
                selectedFiat = userFiat
            )
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return Single.zip(
            sourceAccount.balanceRx().firstOrError(),
            feeOptions,
        ) { balance, fees ->
            val fees = fees[pendingTx.feeSelection.selectedLevel] ?: Money.zero(sourceAsset)

            pendingTx.copy(
                amount = amount,
                totalBalance = balance.total,
                availableBalance = Money.max(balance.withdrawable - fees, Money.zero(sourceAsset)),
                feeForFullAvailable = fees,
                feeAmount = fees,
            )
        }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateOptions(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun getMemoOption(pendingTx: PendingTx) =
        pendingTx.memo

    private fun validateOptions(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (!isMemoValid(getMemoOption(pendingTx))) {
                throw TxValidationFailure(ValidationState.OPTION_INVALID)
            }
        }

    private fun isMemoValid(memoConfirmation: String?): Boolean {
        return if (memoConfirmation.isNullOrBlank()) {
            true
        } else {
            when (sourceAsset.networkTicker) {
                "STX" -> memoConfirmation.length in 1..34
                else -> memoConfirmation.length in 1..28
            }
        }
    }

    private fun signAndPushTx(buildTxResponse: BuildTxResponse): Single<PushTxResponse> {
        val txSignatures = signTransaction(buildTxResponse)
        return rxSingleOutcome {
            nonCustodialService.pushTransaction(
                currency = sourceAsset.networkTicker,
                rawTx = buildTxResponse.rawTx,
                signatures = txSignatures
            )
        }
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createTransaction(pendingTx).flatMap {
            signAndPushTx(it)
        }
            .onErrorResumeNext {
                Logger.e(it)
                Single.error(TransactionError.ExecutionFailed)
            }
            .flatMap { response ->
                // Refresh the balances
                sourceAccount.balanceRx(FreshnessStrategy.Fresh).firstOrError().ignoreElement().onErrorComplete()
                    .thenSingle { Single.just(response) }
            }
            .flatMap {
                Single.just(TxResult.HashedTxResult(it.txId, pendingTx.amount))
            }

    private fun signTransaction(buildTxResponse: BuildTxResponse): List<TransactionSignature> {
        return buildTxResponse.preImages.map { unsignedPreImage ->
            when (unsignedPreImage.signatureAlgorithm) {
                SignatureAlgorithm.SECP256K1 -> {
                    val signature = (sourceAccount as? DynamicNonCustodialAccount)?.signedRawPreImage(
                        unsignedPreImage.rawPreImage
                    ) ?: throw IllegalStateException("Source account is not DynamicNonCustodialAccount")
                    TransactionSignature(
                        preImage = unsignedPreImage.rawPreImage,
                        signingKey = unsignedPreImage.signingKey,
                        signatureAlgorithm = unsignedPreImage.signatureAlgorithm,
                        signature = signature
                    )
                }
                else -> throw TransactionError.ExecutionFailed
            }
        }
    }

    private fun createTransaction(pendingTx: PendingTx) =
        rxSingleOutcome {
            val targetAddress = (txTarget as CryptoAddress)
            nonCustodialService.buildTransaction(
                currency = sourceAsset.networkTicker,
                accountIndex = 0,
                type = TX_TYPE_PAYMENT,
                transactionTarget = targetAddress.address,
                amount = if (pendingTx.amount <= pendingTx.totalBalance) {
                    pendingTx.amount.toBigInteger().toString()
                } else {
                    MAX_AMOUNT
                },
                fee = pendingTx.feeSelection.selectedLevel.toDomain(),
                memo = pendingTx.memo ?: targetAddress.memo ?: "",
                feeCurrency = feeCurrency.networkTicker
            )
        }

    private fun buildConfirmationTotal(pendingTx: PendingTx): TxConfirmationValue.Total {
        val fiatAmount = pendingTx.amount.toUserFiat(exchangeRates) as FiatValue
        val fiatFees = pendingTx.feeAmount.toUserFiat(exchangeRates) as FiatValue
        return TxConfirmationValue.Total(
            totalWithFee = pendingTx.amount + pendingTx.feeAmount,
            exchange = fiatAmount + fiatFees
        )
    }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= Money.zero(sourceAsset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        sourceAccount.balanceRx().firstOrError().map { it.withdrawable }
            .map { balance ->
                if (pendingTx.feeAmount + pendingTx.amount > balance) {
                    throw TxValidationFailure(
                        ValidationState.INSUFFICIENT_FUNDS
                    )
                } else {
                    true
                }
            }.ignoreElement()

    companion object {
        private const val MAX_AMOUNT = "MAX"
        private const val TX_TYPE_PAYMENT = "PAYMENT"
    }

    private fun com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel.toPresentation(): FeeLevel? =
        when (this) {
            com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel.LOW -> null
            com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel.NORMAL -> FeeLevel.Regular
            com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel.HIGH -> FeeLevel.Priority
        }

    private fun FeeLevel.toDomain(): com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel =
        when (this) {
            FeeLevel.None -> throw UnsupportedOperationException()
            FeeLevel.Regular -> com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel.NORMAL
            FeeLevel.Priority -> com.blockchain.core.chains.dynamicselfcustody.domain.model.FeeLevel.HIGH
            FeeLevel.Custom -> throw UnsupportedOperationException()
        }
}
