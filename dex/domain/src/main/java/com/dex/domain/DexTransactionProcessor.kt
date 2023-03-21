package com.dex.domain

import com.blockchain.extensions.safeLet
import com.blockchain.outcome.Outcome
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DexTransactionProcessor(
    private val dexQuotesService: DexQuotesService,
    private val balanceService: DexBalanceService
) {

    private val _dexTransaction: MutableStateFlow<DexTransaction> = MutableStateFlow(
        EmptyDexTransaction
    )

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    val transaction: Flow<DexTransaction>
        get() = _dexTransaction.validate()

    fun initTransaction(sourceAccount: DexAccount) {
        _dexTransaction.update {
            EmptyDexTransaction.copy(
                _sourceAccount = sourceAccount
            )
        }
        startQuotesUpdates()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startQuotesUpdates() {
        scope.launch {
            transaction.mapLatest {
                it.quoteParams
            }.filterNotNull()
                .distinctUntilChanged()
                .debounce(500)
                .mapLatest {
                    if (it.amount.isPositive) {
                        dexQuotesService.quote(
                            it
                        )
                    } else {
                        Outcome.Success(
                            DexQuote.InvalidQuote
                        )
                    }
                }.collectLatest {
                    when (it) {
                        is Outcome.Success -> updateTxQuote(it.value)
                        is Outcome.Failure -> {
                            println(
                                "Quote failed ${it.failure}"
                            )
                        }
                    }
                }
        }
    }

    private fun updateTxQuote(quote: DexQuote) {
        _dexTransaction.update {
            when (quote) {
                is DexQuote.ExchangeQuote -> it.copy(
                    outputAmount = quote.outputAmount,
                    fees = quote.fees
                )
                DexQuote.InvalidQuote -> it.copy(
                    outputAmount = null,
                    fees = null
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
            tx.validateSufficientNetworkFees()
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

data class DexTransaction internal constructor(
    private val _amount: Money?,
    val outputAmount: OutputAmount?,
    private val _sourceAccount: DexAccount?,
    val destinationAccount: DexAccount?,
    val fees: Money?,
    val slippage: Double?,
    val maxAvailable: Money?,
    val txError: DexTxError
) {
    val quoteParams: DexQuoteParams?
        get() = safeLet(_sourceAccount, destinationAccount, _amount) { s, d, a ->
            return DexQuoteParams(
                sourceAccount = s,
                destinationAccount = d,
                amount = a,
                slippage = 0.03
            )
        }

    val sourceAccount: DexAccount
        get() = _sourceAccount ?: throw IllegalStateException("Source Account not initialised")
    val amount: Money
        get() = _amount ?: Money.zero(sourceAccount.currency)
}

data class OutputAmount(val expectedOutput: Money, val minOutputAmount: Money) {
    companion object {
        fun zero(currency: Currency): OutputAmount =
            OutputAmount(
                expectedOutput = Money.zero(currency),
                minOutputAmount = Money.zero(currency),
            )
    }
}

sealed class DexTxError {
    object NotEnoughFunds : DexTxError()
    object NotEnoughGas : DexTxError()
    object None : DexTxError()
}

private val EmptyDexTransaction = DexTransaction(
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    DexTxError.None
)
