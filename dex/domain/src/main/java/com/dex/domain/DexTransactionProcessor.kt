package com.dex.domain

import com.blockchain.extensions.safeLet
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrNull
import info.blockchain.balance.Money
import info.blockchain.balance.isNetworkNativeAsset
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
* https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
* */
@OptIn(FlowPreview::class)
class DexTransactionProcessor(
    private val dexQuotesService: DexQuotesService,
    private val balanceService: DexBalanceService,
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
    private fun startQuotesUpdates() {
        scope.launch {
            transaction
                .mapLatest {
                    it.quoteParams to it.canBeQuoted()
                }.filterNotNull()
                .distinctUntilChanged { (oldParams, _), (newParams, _) ->
                    oldParams == newParams
                }
                .debounce(350)
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
                        is Outcome.Success -> updateTxQuote(it.value)
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
                outputAmount = null,
                fees = null
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

    private fun updateTxQuote(quote: DexQuote) {
        _dexTransaction.update {
            when (quote) {
                is DexQuote.ExchangeQuote -> it.copy(
                    outputAmount = quote.outputAmount,
                    fees = quote.fees,
                    quoteError = null
                )
                DexQuote.InvalidQuote -> it.copy(
                    outputAmount = null,
                    fees = null,
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
                destinationAccount = if (dexTx.destinationAccount?.currency == sourceAccount.currency) {
                    null
                } else dexTx.destinationAccount
            )
        }
    }

    fun updateDestinationAccount(destinationAccount: DexAccount?) {
        _dexTransaction.update {
            it.copy(
                destinationAccount = destinationAccount
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
        return fees?.let {
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
    amount.isPositive && (
        this.txError == DexTxError.None ||
            this.txError is DexTxError.QuoteError
        )

data class DexTransaction internal constructor(
    private val _amount: Money?,
    val outputAmount: OutputAmount?,
    private val _sourceAccount: DexAccount?,
    val destinationAccount: DexAccount?,
    val fees: Money?,
    val slippage: Double,
    val maxAvailable: Money?,
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
    null,
    0.toDouble(),
    null,
    DexTxError.None,
    null
)

sealed class DexTxError {
    object NotEnoughFunds : DexTxError()
    object NotEnoughGas : DexTxError()
    data class QuoteError(val title: String, val message: String) : DexTxError()
    object TokenNotAllowed : DexTxError()
    class FatalTxError(val exception: Exception) : DexTxError()
    object None : DexTxError()
}
