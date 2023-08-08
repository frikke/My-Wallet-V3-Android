package com.dex.domain

import com.blockchain.core.chains.ethereum.EvmNetworkPreImageSigner
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.extensions.safeLet
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.getOrNull
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Money
import info.blockchain.balance.isNetworkNativeAsset
import java.math.BigDecimal
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DexTransactionProcessor(
    private val dexQuotesService: DexQuotesService,
    private val balanceService: DexBalanceService,
    private val unifiedActivityService: UnifiedActivityService,
    private val dexTransactionService: DexTransactionService,
    private val evmNetworkSigner: EvmNetworkPreImageSigner,
    private val allowanceService: AllowanceService,
    private val notificationTransmitter: NotificationTransmitter
) {

    private val _isFetchingQuote: MutableStateFlow<Boolean> = MutableStateFlow(
        false
    )

    val quoteFetching: Flow<Boolean>
        get() = _isFetchingQuote

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val _dexTransaction: MutableStateFlow<DexTransaction> = MutableStateFlow(
        EmptyDexTransaction
    )

    private val revalidateSignal = MutableStateFlow(Revalidate())

    private val _transactionSharedFlow: SharedFlow<DexTransaction> =
        combine(_dexTransaction, revalidateSignal) { tx, _ ->
            tx
        }.validate()
            .flowOn(Dispatchers.IO)
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000),
                replay = 1
            )

    val transaction: Flow<DexTransaction>
        get() = _transactionSharedFlow

    fun dispose() {
        job?.cancel()
    }

    fun initTransaction(sourceAccount: DexAccount, destinationAccount: DexAccount?, slippage: Double) {
        _dexTransaction.update {
            EmptyDexTransaction.copy(
                _sourceAccount = sourceAccount,
                destinationAccount = destinationAccount,
                slippage = slippage
            )
        }
        startQuotesUpdates()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val quoteInput: Flow<Pair<DexQuoteParams?, Boolean>>
        get() = transaction.mapLatest {
            it.quoteParams() to it.canBeQuoted()
        }.filterNotNull()
            .distinctUntilChanged { (oldParams, _), (newParams, _) ->
                oldParams == newParams
            }
            .debounce(350)

    private val _quoteTtl = MutableStateFlow(QuoteTTL())
    private val activeSubscriptions = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val updateQuote: Flow<Unit>
        get() = _quoteTtl
            .mapLatest {
                delay(it.ttl)
            }
            .filter { activeSubscriptions.value > 0 }
            .onStart {
                emit(Unit)
            }

    private val hasActiveSubscribers: Flow<Boolean>
        get() = activeSubscriptions.map { it > 0 }.debounce(1000).distinctUntilChanged().filter { it }

    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startQuotesUpdates() {
        job?.cancel()
        job = scope.launch {
            combine(
                quoteInput,
                updateQuote,
                hasActiveSubscribers,
            ) { input, _, _ ->
                input
            }
                .mapLatest { (params, canBeQuoted) ->
                    if (params != null && canBeQuoted) {
                        _isFetchingQuote.emit(true)
                        dexQuotesService.quote(params)
                    } else {
                        Outcome.Success(
                            DexQuote.InvalidQuote
                        )
                    }
                }.onEach {
                    _isFetchingQuote.emit(false)
                }.collectLatest { outcome ->
                    when (outcome) {
                        is Outcome.Success -> {
                            (outcome.value as? DexQuote.ExchangeQuote)?.let { exchangeQuote ->
                                _quoteTtl.emit(QuoteTTL(exchangeQuote.quoteTtl))
                            }
                            updateTxQuote(outcome.value)
                        }

                        is Outcome.Failure -> {
                            (outcome.failure as? DexTxError.QuoteError)?.let { qError ->
                                updateQuoteError(qError)
                            }
                        }
                    }
                }
        }
    }

    private fun updateQuoteError(failure: DexTxError.QuoteError) {
        _dexTransaction.update {
            it.copy(
                quoteError = failure,
                quote = null,
            )
        }
    }

    suspend fun revalidate() {
        revalidateSignal.emit(Revalidate())
    }

    fun updateSlippage(slippage: Double) {
        _dexTransaction.update {
            it.copy(
                slippage = slippage
            )
        }
    }

    suspend fun execute() {
        try {
            val transaction = _dexTransaction.value
            val coinNetwork = transaction.sourceAccount.currency.coinNetwork
            check(coinNetwork != null)
            _dexTransaction.update {
                it.copy(
                    txResult = dexTransactionService.buildTx(transaction).flatMap { builtTx ->
                        dexTransactionService.pushTx(
                            coinNetwork = coinNetwork,
                            rawTx = builtTx.rawTx,
                            signatures = builtTx.preImages.map { unsignedPreImage ->
                                evmNetworkSigner.signPreImage(unsignedPreImage)
                            }
                        ).doOnSuccess {
                            notificationTransmitter.postEvent(NotificationEvent.NonCustodialTransaction)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            _dexTransaction.update {
                it.copy(
                    txResult = Outcome.Failure(Exception(e))
                )
            }
        }
    }

    private fun updateTxQuote(quote: DexQuote) {
        _dexTransaction.update {
            when (quote) {
                is DexQuote.ExchangeQuote -> it.copy(
                    quote = quote,
                    quoteError = null
                )

                DexQuote.InvalidQuote -> it.copy(
                    quote = null,
                    quoteError = null
                )
            }
        }
    }

    /*
    *
    * when updating the source account, we check if the sell amount is what user has typed,
    * if yes, we just change the amount to the selected currency otherwise we just set the quote (if any) to zero
    * */
    fun updateSourceAccount(sourceAccount: DexAccount) {
        _dexTransaction.update { dexTx ->
            val sourceIsSameAsDestination = dexTx.destinationAccount?.currency == sourceAccount.currency
            val updatedAmount = when (dexTx.inputAmount) {
                is ExchangeAmount.SellAmount -> ExchangeAmount.SellAmount(
                    Money.fromMajor(
                        sourceAccount.currency,
                        dexTx.inputAmount.amount.toBigDecimal()
                    ),
                    null
                )

                is ExchangeAmount.BuyAmount -> if (!sourceIsSameAsDestination) dexTx.inputAmount else null
                else -> null
            }

            dexTx.copy(
                _sourceAccount = sourceAccount,
                inputAmount = updatedAmount,
                quote = if (dexTx.inputAmount is ExchangeAmount.BuyAmount) null else dexTx.quote,
                quoteError = null,
                destinationAccount = if (sourceIsSameAsDestination) {
                    null
                } else dexTx.destinationAccount,
            )
        }
    }

    fun updateDestinationAccount(destinationAccount: DexAccount?) {
        _dexTransaction.update {
            it.copy(
                destinationAccount = destinationAccount,
                quoteError = null,
            )
        }
    }

    fun updateSellAmount(amount: BigDecimal) {
        _dexTransaction.update { tx ->
            tx.copy(
                inputAmount = ExchangeAmount.SellAmount(Money.fromMajor(tx.sourceAccount.currency, amount), null),
            )
        }
    }

    fun updateBuyAmount(amount: BigDecimal) {
        _dexTransaction.update { tx ->
            val destinationCurrency = tx.destinationAccount?.currency ?: return@update tx
            tx.copy(
                inputAmount = ExchangeAmount.BuyAmount(Money.fromMajor(destinationCurrency, amount), null),
            )
        }
    }

    suspend fun subscribeForTxUpdates() {
        activeSubscriptions.emit(activeSubscriptions.value + 1)
    }

    suspend fun unsubscribeToTxUpdates() {
        activeSubscriptions.emit(activeSubscriptions.value - 1)
    }

    private fun Flow<DexTransaction>.validate(): Flow<DexTransaction> {
        return map { tx ->
            tx.ensureBalancesUpToDate()
        }.map { tx ->
            tx.copy(
                txErrors = emptyList()
            )
        }.map { tx ->
            tx.validateSufficientFunds()
        }.map { tx ->
            tx.validateAllowance()
        }.map { tx ->
            tx.validateQuoteError()
        }.map { tx ->
            tx.validateSufficientNetworkFees()
        }.map { tx ->
            tx.validateTransactionInProcess()
        }
    }

    private fun DexTransaction.validateQuoteError(): DexTransaction {
        return if (quoteError == null)
            this
        else copy(txErrors = txErrors.plus(quoteError))
    }

    private suspend fun DexTransaction.ensureBalancesUpToDate(): DexTransaction {
        val sourceAccountBalance =
            sourceAccount.account.balance(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).firstOrNull()
        val destinationAccountBalance =
            destinationAccount?.account?.balance(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
                ?.firstOrNull()
        return this.copy(
            _sourceAccount = sourceAccount.copy(
                balance = sourceAccountBalance?.total ?: sourceAccount.balance,
                fiatBalance = sourceAccountBalance?.totalFiat ?: sourceAccount.fiatBalance,
            ),
            destinationAccount = destinationAccount?.copy(
                balance = destinationAccountBalance?.total ?: destinationAccount.balance,
                fiatBalance = destinationAccountBalance?.totalFiat ?: destinationAccount.fiatBalance,
            )
        )
    }

    private suspend fun DexTransaction.validateTransactionInProcess(): DexTransaction {
        val txNetwork = sourceAccount.currency.coinNetwork ?: return this
        val transactions =
            unifiedActivityService.getAllActivity().filter { it != DataResource.Loading }.first().dataOrElse(
                emptyList()
            ).filter { it.network == txNetwork.networkTicker }

        return if (transactions.any { it.status in listOf("PENDING", "CONFIRMING") }) {
            copy(txErrors = txErrors.plus(DexTxError.TxInProgress(txNetwork)))
        } else {
            this
        }
    }

    private suspend fun DexTransaction.validateAllowance(): DexTransaction {
        return if (
            destinationAccount == null ||
            inputAmount?.amount?.isPositive == false ||
            sourceAccount.currency.isNetworkNativeAsset()
        ) {
            this
        } else {
            val allowance = allowanceService.tokenAllowance(
                sourceAccount.currency
            ).getOrNull()
            allowance?.let {
                if (!it.isTokenAllowed)
                    return copy(
                        txErrors = txErrors.plus(
                            DexTxError.TokenNotAllowed(
                                allowanceService.isAllowanceApprovedButPending(sourceAccount.currency)
                            )
                        )
                    )
                else this
            } ?: return this
        }
    }

    private fun DexTransaction.validateSufficientFunds(): DexTransaction {
        val sellAmount =
            (inputAmount as? ExchangeAmount.SellAmount)?.amount ?: quote?.sellAmount?.amount ?: return this
        return if (sourceAccount.balance >= sellAmount)
            this
        else copy(
            txErrors = txErrors.plus(DexTxError.NotEnoughFunds)
        )
    }

    private suspend fun DexTransaction.validateSufficientNetworkFees(): DexTransaction {
        if (inputAmount !is ExchangeAmount.SellAmount) return this
        return quote?.networkFees?.let {
            val networkBalance = balanceService.networkBalance(sourceAccount)
            val availableForFees =
                if (sourceAccount.currency.isNetworkNativeAsset()) networkBalance.minus(
                    inputAmount.amount
                ) else networkBalance
            if (availableForFees >= it) {
                this
            } else copy(
                txErrors = txErrors.plus(DexTxError.NotEnoughGas)
            )
        } ?: this
    }
}

private fun DexTransaction.quoteParams(): DexQuoteParams? {
    return safeLet(sourceAccount, destinationAccount, inputAmount) { s, d, a ->
        return DexQuoteParams(
            sourceAccount = s,
            destinationAccount = d,
            inputAmount = a,
            slippage = slippage,
            sourceHasBeenAllowed = txErrors.any { it is DexTxError.TokenNotAllowed }.not()
        )
    }
}

/**
 * We are able to fetch a quote when amount > 0 AND ( no error or error is quote related )
 */
private fun DexTransaction.canBeQuoted() =
    this.inputAmount?.amount?.isPositive == true && txErrors.all { it.allowsQuotesFetching }

data class DexTransaction internal constructor(
    val inputAmount: ExchangeAmount?,
    val quote: DexQuote.ExchangeQuote?,
    private val _sourceAccount: DexAccount?,
    val destinationAccount: DexAccount?,
    val txResult: Outcome<Exception, String>?,
    val slippage: Double,
    val txErrors: List<DexTxError>,
    val quoteError: DexTxError.QuoteError?
) {
    val sourceAccount: DexAccount
        get() = _sourceAccount ?: throw IllegalStateException("Source Account not initialised")

    fun hasBeenExecuted() =
        txResult != null
}

private val EmptyDexTransaction = DexTransaction(
    null,
    null,
    null,
    null,
    null,
    0.toDouble(),
    listOf(),
    null,
)

/*
* This class represents the constant amount that is given to quote as input. Is the amount user inputed in the
* text-field
* */
sealed class ExchangeAmount {
    abstract val amount: Money
    abstract val minAmount: Money?
    val isPositive: Boolean
        get() = amount.isPositive

    data class SellAmount(override val amount: Money, override val minAmount: Money?) : ExchangeAmount()
    data class BuyAmount(override val amount: Money, override val minAmount: Money?) : ExchangeAmount()
}

sealed class DexTxError {

    abstract val allowsQuotesFetching: Boolean

    object NotEnoughFunds : DexTxError() {
        override val allowsQuotesFetching: Boolean
            get() = false
    }

    object NotEnoughGas : DexTxError() {
        override val allowsQuotesFetching: Boolean
            get() = true
    }

    data class QuoteError(val title: String?, val message: String?, private val id: String?) : DexTxError() {
        override val allowsQuotesFetching: Boolean
            get() = true

        val isInsufficientFundsError: Boolean
            get() = id == "dex.quote.insufficient.funds"
    }

    data class TokenNotAllowed(val hasBeenApproved: Boolean) : DexTxError() {
        override val allowsQuotesFetching: Boolean
            get() = true
    }

    class FatalTxError(val exception: Exception) : DexTxError() {
        override val allowsQuotesFetching: Boolean
            get() = false
    }

    class TxInProgress(val coinNetwork: CoinNetwork) : DexTxError() {
        override val allowsQuotesFetching: Boolean
            get() = true
    }
}

private class QuoteTTL(val ttl: Long = 0L) {
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Random.nextInt()
    }
}

private class Revalidate {
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Random.nextInt()
    }
}
