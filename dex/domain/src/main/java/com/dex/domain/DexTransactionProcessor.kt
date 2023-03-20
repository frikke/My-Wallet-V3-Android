package com.dex.domain

import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DexTransactionProcessor {

    private val _dexTransaction: MutableStateFlow<DexTransaction> = MutableStateFlow(
        EmptyDexTransaction
    )

    val transaction: Flow<DexTransaction>
        get() = _dexTransaction.asStateFlow()

    fun initTransaction() =
        _dexTransaction.update { EmptyDexTransaction }

    fun updateSourceAccount(sourceAccount: DexAccount) {
        _dexTransaction.update { dexTx ->
            dexTx.copy(
                sourceAccount = sourceAccount,
                amount = dexTx.amount?.let { current ->
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
            tx.sourceAccount?.let {
                tx.copy(
                    amount = Money.fromMajor(it.currency, amount)
                )
            } ?: tx
        }
    }
}

data class DexTransaction(
    val amount: Money?,
    val sourceAccount: DexAccount?,
    val destinationAccount: DexAccount?,
    val fees: Money?,
    val slippage: Double?,
    val maxAvailable: Money?
)

val EmptyDexTransaction = DexTransaction(
    null,
    null,
    null,
    null,
    null,
    null
)
