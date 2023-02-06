package com.blockchain.coincore.selfcustody

import com.blockchain.api.selfcustody.BuildTxResponse
import com.blockchain.api.selfcustody.PreImage
import com.blockchain.api.selfcustody.PushTxResponse
import com.blockchain.api.selfcustody.SignatureAlgorithm
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
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
import com.blockchain.logging.Logger
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.then
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash

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

class DynamicOnChanTxEngine(
    private val nonCustodialService: NonCustodialService,
    walletPreferences: WalletStatusPrefs,
    requireSecondPassword: Boolean,
    resolvedAddress: Single<String>
) : OnChainTxEngineBase(requireSecondPassword, walletPreferences, resolvedAddress) {

    private val feeCurrency: AssetInfo by lazy {
        nonCustodialService.getFeeCurrencyFor(sourceAsset as AssetInfo)
    }

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(txTarget, AssetAction.Send, sourceAccount),
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
        Single.just(
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset),
                feeAmount = Money.zero(feeCurrency),
                feeSelection = FeeSelection(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = feeCurrency
                ),
                selectedFiat = userFiat
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return sourceAccount.balanceRx().firstOrError().map { balance ->
            pendingTx.copy(
                amount = amount,
                totalBalance = balance.total,
                availableBalance = balance.withdrawable,
                feeForFullAvailable = CryptoValue.zero(feeCurrency),
                feeAmount = CryptoValue.zero(feeCurrency),
                feeSelection = pendingTx.feeSelection.copy()
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
            .flatMap {
                Single.just(TxResult.HashedTxResult(it.txId, pendingTx.amount))
            }

    private fun signTransaction(buildTxResponse: BuildTxResponse): List<TransactionSignature> {
        val signingKey = (sourceAccount as? DynamicNonCustodialAccount)?.getSigningKey()?.toECKey()
            ?: throw IllegalStateException("Source account is not DynamicNonCustodialAccount")
        return buildTxResponse.preImages.map { unsignedPreImage ->
            when (unsignedPreImage.signatureAlgorithm) {
                SignatureAlgorithm.SECP256K1 -> {
                    val signature = getSignature(unsignedPreImage, signingKey)
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

    private fun getSignature(unsignedPreImage: PreImage, signingKey: ECKey): String {
        val hash = Sha256Hash.wrap(unsignedPreImage.rawPreImage)
        val resultSignature = signingKey.sign(hash)
        val r = resultSignature.r.toPaddedHexString()
        val s = resultSignature.s.toPaddedHexString()
        val v = "0${signingKey.findRecoveryId(hash, resultSignature)}"
        return r + s + v
    }

    private fun BigInteger.toPaddedHexString(): String {
        val radix = 16 // For digit to character conversion (digit to hexadecimal in this case)
        val desiredLength = 64
        val padChar = '0'
        return toString(radix).padStart(desiredLength, padChar)
    }

    private fun createTransaction(pendingTx: PendingTx) =
        rxSingleOutcome {
            val targetAddress = (txTarget as CryptoAddress)
            nonCustodialService.buildTransaction(
                currency = sourceAsset.networkTicker,
                accountIndex = 0,
                type = TX_TYPE_PAYMENT,
                transactionTarget = targetAddress.address,
                amount = if (pendingTx.amount < pendingTx.totalBalance) {
                    pendingTx.amount.toBigInteger().toString()
                } else {
                    MAX_AMOUNT
                },
                fee = NORMAL_FEE,
                memo = pendingTx.memo ?: targetAddress.memo ?: "",
                feeCurrency = feeCurrency.networkTicker
            )
        }

    private fun buildConfirmationTotal(pendingTx: PendingTx): TxConfirmationValue.Total {
        val fiatAmount = pendingTx.amount.toUserFiat(exchangeRates) as FiatValue
        return TxConfirmationValue.Total(
            totalWithFee = pendingTx.amount,
            exchange = fiatAmount
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
                if (pendingTx.amount > balance) {
                    throw TxValidationFailure(
                        ValidationState.INSUFFICIENT_FUNDS
                    )
                } else {
                    true
                }
            }.ignoreElement()

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular, FeeLevel.Priority)
        private const val NORMAL_FEE = "NORMAL"
        private const val MAX_AMOUNT = "MAX"
        private const val TX_TYPE_PAYMENT = "PAYMENT"
    }
}
