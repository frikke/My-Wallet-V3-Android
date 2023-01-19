package com.blockchain.bitpay

import com.blockchain.analytics.Analytics
import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.bitpay.analytics.BitPayEvent
import com.blockchain.bitpay.models.BitPayTransaction
import com.blockchain.bitpay.models.BitPaymentRequest
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EngineTransaction
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.logging.Logger
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.store.Store
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.dust.data.DustInput
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import org.bitcoinj.core.Transaction
import org.spongycastle.util.encoders.Hex

const val BITPAY_TIMER_SUB = "bitpay_timer"
private val PendingTx.bitpayTimer: Disposable?
    get() = (this.engineState[BITPAY_TIMER_SUB] as? Disposable)

interface BitPayClientEngine {
    fun doPrepareTransaction(pendingTx: PendingTx): Single<Pair<Transaction, DustInput?>>
    fun doSignTransaction(
        tx: Transaction,
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction>

    fun doOnTransactionSuccess(pendingTx: PendingTx)
    fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable)
}

class BitpayTxEngine(
    private val bitPayDataManager: BitPayDataManager,
    private val assetEngine: OnChainTxEngineBase,
    private val walletPrefs: WalletStatusPrefs,
    private val analytics: Analytics
) : TxEngine() {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    private val balancesCache: Store<BalancesResponse> by scopedInject()

    override fun assertInputsValid() {
        // Only support non-custodial BTC & BCH bitpay at this time
        val supportedCryptoCurrencies = listOf(CryptoCurrency.BTC, CryptoCurrency.BCH)
        check(supportedCryptoCurrencies.contains(sourceAsset))
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is BitPayInvoiceTarget)
        require(assetEngine is BitPayClientEngine)
        assetEngine.assertInputsValid()
    }

    override fun ensureSourceBalanceFreshness() {
        balancesCache.markAsStale()
    }

    private val executionClient: BitPayClientEngine by unsafeLazy {
        assetEngine as BitPayClientEngine
    }

    private val bitpayInvoice: BitPayInvoiceTarget by unsafeLazy {
        txTarget as BitPayInvoiceTarget
    }

    override fun doAfterOnStart(
        sourceAccount: BlockchainAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRatesDataManager,
        refreshTrigger: RefreshTrigger
    ) {
        assetEngine.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        assetEngine.doInitialiseTx()
            .map { tx ->
                tx.copy(
                    amount = bitpayInvoice.amount,
                    feeSelection = tx.feeSelection.copy(
                        selectedLevel = FeeLevel.Priority,
                        availableLevels = AVAILABLE_FEE_LEVELS
                    )
                )
            }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doUpdateAmount(bitpayInvoice.amount, pendingTx)
            .flatMap { assetEngine.doBuildConfirmations(it) }
            .map { pTx ->
                startTimerIfNotStarted(pTx)
            }.map { pTx ->
                pTx.addOrReplaceOption(
                    TxConfirmationValue.BitPayCountdown(timeRemainingSecs()),
                    true
                )
            }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx.addOrReplaceOption(TxConfirmationValue.BitPayCountdown(timeRemainingSecs()), true))

    private fun startTimerIfNotStarted(pendingTx: PendingTx): PendingTx =
        if (pendingTx.bitpayTimer == null) {
            pendingTx.copy(
                engineState = pendingTx.engineState.copyAndPut(
                    BITPAY_TIMER_SUB, startCountdownTimer(timeRemainingSecs())
                )
            )
        } else {
            pendingTx
        }

    private fun timeRemainingSecs() =
        (bitpayInvoice.expireTimeMs - System.currentTimeMillis()) / 1000

    private fun startCountdownTimer(remainingTime: Long): Disposable {
        var remaining = remainingTime
        return Observable.interval(1, TimeUnit.SECONDS)
            .doOnEach { remaining-- }
            .map { remaining }
            .doOnNext { updateCountdownConfirmation() }
            .takeUntil { it <= TIMEOUT_STOP }
            .doOnComplete { handleCountdownComplete() }
            .subscribe()
    }

    private fun updateCountdownConfirmation() {
        refreshConfirmations(false)
    }

    private fun handleCountdownComplete() {
        Logger.d("BitPay Invoice Countdown expired")
        refreshConfirmations(true)
    }

    // Don't set the amount here, it is fixed so we can do it in the confirmation building step
    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        return Single.just(pendingTx)
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doValidateAmount(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateTimeout(pendingTx)
            .flatMap { assetEngine.doValidateAll(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun doValidateTimeout(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)
            .map { pTx ->
                if (timeRemainingSecs() <= TIMEOUT_STOP) {
                    analytics.logEvent(BitPayEvent.InvoiceExpired)
                    throw TxValidationFailure(ValidationState.INVOICE_EXPIRED)
                }
                pTx
            }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        executionClient.doPrepareTransaction(pendingTx)
            .flatMap { (tx, _) ->
                doVerifyTransaction(bitpayInvoice.invoiceId, tx)
            }.flatMap { txVerified ->
                executionClient.doSignTransaction(txVerified, pendingTx, secondPassword)
            }.flatMap { engineTx ->
                doExecuteTransaction(bitpayInvoice.invoiceId, engineTx)
            }.doOnSuccess {
                walletPrefs.setBitPaySuccess()
                analytics.logEvent(BitPayEvent.TxSuccess(pendingTx.amount as CryptoValue))
                executionClient.doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                analytics.logEvent(BitPayEvent.TxFailed(e.message ?: e.toString()))
                executionClient.doOnTransactionFailed(pendingTx, e)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun doVerifyTransaction(
        invoiceId: String,
        tx: Transaction
    ): Single<Transaction> =
        bitPayDataManager.paymentVerificationRequest(
            invoiceId,
            BitPaymentRequest(
                sourceAsset.networkTicker,
                listOf(
                    BitPayTransaction(
                        String(Hex.encode(tx.bitcoinSerialize())),
                        tx.messageSize
                    )
                )
            )
        ).andThen(Single.just(tx))

    private fun doExecuteTransaction(
        invoiceId: String,
        tx: EngineTransaction
    ): Single<String> =
        bitPayDataManager.paymentSubmitRequest(
            invoiceId,
            BitPaymentRequest(
                sourceAsset.networkTicker,
                listOf(
                    BitPayTransaction(
                        tx.encodedMsg,
                        tx.msgSize
                    )
                )
            )
        ).andThen(Single.just(tx.txHash))

    companion object {
        private const val TIMEOUT_STOP = 2
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Priority)
    }
}
