package com.dex.domain

import com.blockchain.core.chains.ethereum.EvmNetworkPreImageSigner
import com.blockchain.extensions.safeLet
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrNull
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
    private val dexTransactionService: DexTransactionService,
    private val evmNetworkSigner: EvmNetworkPreImageSigner,
    private val allowanceService: AllowanceService
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

    private val _transactionSharedFlow: SharedFlow<DexTransaction> =
        _dexTransaction.validate()
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000),
                replay = 1
            )

    val transaction: Flow<DexTransaction>
        get() = _transactionSharedFlow

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
            it.quoteParams to it.canBeQuoted()
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
        get() = activeSubscriptions.map { it > 0 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startQuotesUpdates() {
        scope.launch {
            combine(
                quoteInput,
                updateQuote,
                hasActiveSubscribers.debounce(1000).distinctUntilChanged().filter { it }
            ) { input, _, _ ->
                input
            }
                .mapLatest { (params, canBeQuoted) ->
                    if (params != null && canBeQuoted) {
                        _isFetchingQuote.emit(true)
                        dexQuotesService.quote(
                            params
                        )
                    } else {
                        Outcome.Success(
                            DexQuote.InvalidQuote
                        )
                    }
                }.onEach {
                    _isFetchingQuote.emit(false)
                }.collectLatest {
                    when (it) {
                        is Outcome.Success -> {
                            _quoteTtl.emit(QuoteTTL(15000))
                            updateTxQuote(it.value)
                        }
                        is Outcome.Failure -> {
                            (it.failure as? DexTxError.QuoteError)?.let { qError ->
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

    fun updateSlippage(slippage: Double) {
        _dexTransaction.update {
            it.copy(
                slippage = slippage
            )
        }
    }

    suspend fun execute() {
        val transaction = _dexTransaction.value
        val coinNetwork = transaction.sourceAccount.currency.coinNetwork
        check(coinNetwork != null)
        val builtTx = dexTransactionService.buildTx(_dexTransaction.value).getOrNull()
        builtTx?.let {
            dexTransactionService.pushTx(
                coinNetwork = coinNetwork,
                rawTx = it.rawTx,
                signatures = it.preImages.map { unsignedPreImage ->
                    evmNetworkSigner.signPreImage(unsignedPreImage)
                }
            )
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

    fun updateSourceAccount(sourceAccount: DexAccount) {
        _dexTransaction.update { dexTx ->
            dexTx.copy(
                _sourceAccount = sourceAccount,
                _amount = dexTx.amount.let { current ->
                    Money.fromMajor(
                        sourceAccount.currency,
                        current.toBigDecimal()
                    )
                },
                quoteError = null,
                destinationAccount = if (dexTx.destinationAccount?.currency == sourceAccount.currency) {
                    null
                } else dexTx.destinationAccount
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

    fun updateTransactionAmount(amount: BigDecimal) {
        _dexTransaction.update { tx ->
            tx.copy(
                _amount = Money.fromMajor(tx.sourceAccount.currency, amount),
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
            tx.copy(
                txError = DexTxError.None
            )
        }.validate { tx ->
            tx.validateSufficientFunds()
        }.validate { tx ->
            tx.validateAllowance()
        }.validate { tx ->
            tx.validateSufficientNetworkFees()
        }.validate { tx ->
            tx.validateQuoteError()
        }
        /* add rest off validations*/
    }

    private fun Flow<DexTransaction>.validate(validation: suspend (DexTransaction) -> DexTransaction):
        Flow<DexTransaction> {
        return map {
            if (it.txError == DexTxError.None)
                validation(it)
            else it
        }
    }

    private fun DexTransaction.validateQuoteError(): DexTransaction {
        return if (quoteError == null)
            this
        else copy(
            txError = quoteError
        )
    }

    private suspend fun DexTransaction.validateAllowance(): DexTransaction {
        return if (destinationAccount == null || !amount.isPositive || sourceAccount.currency.isNetworkNativeAsset()) {
            this
        } else {
            val allowance = allowanceService.tokenAllowance(
                sourceAccount.currency
            ).getOrNull()
            allowance?.let {
                if (it.allowanceAmount == "0")
                    return copy(
                        txError = DexTxError.TokenNotAllowed
                    )
                else this
            } ?: return this
        }
    }

    private fun DexTransaction.validateSufficientFunds(): DexTransaction {
        return if (sourceAccount.balance >= amount)
            this
        else copy(
            txError = DexTxError.NotEnoughFunds
        )
    }

    private suspend fun DexTransaction.validateSufficientNetworkFees(): DexTransaction {
        return quote?.networkFees?.let {
            val networkBalance = balanceService.networkBalance(sourceAccount)
            if (networkBalance >= it) {
                this
            } else copy(
                txError = DexTxError.NotEnoughGas
            )
        } ?: this
    }
}

/**
 * We are able to fetch a quote when amount > 0 AND ( no error or error is quote related )
 */
private fun DexTransaction.canBeQuoted() =
    amount.isPositive && txError.allowsQuotesFetching()

data class DexTransaction internal constructor(
    private val _amount: Money?,
    val quote: DexQuote.ExchangeQuote?,
    private val _sourceAccount: DexAccount?,
    val destinationAccount: DexAccount?,
    val slippage: Double,
    val txError: DexTxError,
    val quoteError: DexTxError.QuoteError?
) {
    val quoteParams: DexQuoteParams?
        get() = safeLet(_sourceAccount, destinationAccount, _amount) { s, d, a ->
            return DexQuoteParams(
                sourceAccount = s,
                destinationAccount = d,
                amount = a,
                slippage = slippage
            )
        }

    val sourceAccount: DexAccount
        get() = _sourceAccount ?: throw IllegalStateException("Source Account not initialised")
    val amount: Money
        get() = _amount ?: Money.zero(sourceAccount.currency)
}

data class OutputAmount(val expectedOutput: Money, val minOutputAmount: Money)

private val EmptyDexTransaction = DexTransaction(
    null,
    null,
    null,
    null,
    0.toDouble(),
    DexTxError.None,
    null,
)

sealed class DexTxError {
    fun allowsQuotesFetching(): Boolean {
        return this == None || this == TokenNotAllowed || this is QuoteError
    }

    object NotEnoughFunds : DexTxError()
    object NotEnoughGas : DexTxError()
    data class QuoteError(val title: String, val message: String) : DexTxError() {

        fun isLiquidityError(): Boolean =
            message.contains("INSUFFICIENT_ASSET_LIQUIDITY", true)
    }

    object TokenNotAllowed : DexTxError()
    class FatalTxError(val exception: Exception) : DexTxError()
    object None : DexTxError()
}

private class QuoteTTL(val ttl: Long = 0L) {
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Random.nextInt()
    }
}
