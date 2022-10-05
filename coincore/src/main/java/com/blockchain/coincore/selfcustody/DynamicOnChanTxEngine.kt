package com.blockchain.coincore.selfcustody

import com.blockchain.api.selfcustody.PreImage
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
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import kotlinx.serialization.json.JsonObject
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

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
        return createTransaction(pendingTx)
            .map { response ->
                val estimatedFee = Money.fromMinor(sourceAsset, response.summary.absoluteFeeEstimate.toBigInteger())
                pendingTx.copy(
                    confirmations = listOfNotNull(
                        TxConfirmationValue.From(sourceAccount, sourceAsset),
                        TxConfirmationValue.To(txTarget, AssetAction.Send, sourceAccount),
                        TxConfirmationValue.CompoundNetworkFee(
                            sendingFeeInfo = if (!estimatedFee.isZero) {
                                FeeInfo(
                                    feeAmount = estimatedFee,
                                    fiatAmount = estimatedFee.toUserFiat(exchangeRates),
                                    asset = sourceAsset
                                )
                            } else null,
                            feeLevel = pendingTx.feeSelection.selectedLevel
                        ),
                        buildConfirmationTotal(pendingTx)
                    ),
                    engineState = pendingTx.engineState.plus(
                        mapOf(PREIMAGES to response.preImages, RAWTX to response.rawTx)
                    )
                )
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

        return sourceAccount.balanceRx.firstOrError().map { balance ->
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
            .updateTxValidity(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        rxSingleOutcome {
            val txSignatures = signTransaction(pendingTx)
            nonCustodialService.pushTransaction(
                currency = sourceAsset.networkTicker,
                rawTx = pendingTx.engineState[RAWTX] as? JsonObject ?: throw TransactionError.ExecutionFailed,
                signatures = txSignatures
            )
        }
            .onErrorResumeNext {
                Timber.e(it)
                Single.error(TransactionError.ExecutionFailed)
            }
            .flatMap {
                Single.just(TxResult.HashedTxResult(it.txId, pendingTx.amount))
            }

    private fun signTransaction(pendingTx: PendingTx): List<TransactionSignature> {
        val signingKey = (sourceAccount as? DynamicNonCustodialAccount)?.getSigningKey()?.toECKey()
            ?: throw IllegalStateException("Source account is not DynamicNonCustodialAccount")
        return (pendingTx.engineState[PREIMAGES] as? List<PreImage>)?.map { unsignedPreImage ->
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
        } ?: throw TransactionError.ExecutionFailed
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
                memo = targetAddress.memo ?: "",
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
        sourceAccount.balanceRx.firstOrError().map { it.withdrawable }
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
        private const val PREIMAGES = "PREIMAGES"
        private const val NORMAL_FEE = "NORMAL"
        private const val MAX_AMOUNT = "MAX"
        private const val TX_TYPE_PAYMENT = "PAYMENT"
        private const val RAWTX = "RAWTX"
    }
}
