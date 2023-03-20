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
                    dexQuotesService.quote(
                        it
                    )
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
            it.copy(
                outputAmount = quote.outputAmount
            )
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
                outputAmount = tx.outputAmount?.let { cAmount ->
                    if (amount.signum() == 0) {
                        OutputAmount.zero(cAmount.expectedOutput.currency)
                    } else cAmount
                }
            )
        }
    }

    private fun Flow<DexTransaction>.validate(): Flow<DexTransaction> {
        return map { tx ->
            tx.validateSufficientFunds()
        }
        /**
         add rest off validations
         */
    }

    private fun DexTransaction.validateSufficientFunds(): DexTransaction {
        return if (sourceAccount.balance >= amount)
            this.copy(txError = DexTxError.None)
        else copy(
            txError = DexTxError.NotEnoughFunds
        )
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
        get() = safeLet(_sourceAccount, destinationAccount, _amount.takeIf { it?.isPositive == true }) { s, d, a ->
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
