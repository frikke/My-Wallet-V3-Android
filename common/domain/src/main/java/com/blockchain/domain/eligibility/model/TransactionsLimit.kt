package com.blockchain.domain.eligibility.model

import java.io.Serializable

sealed class TransactionsLimit : Serializable {
    object Unlimited : TransactionsLimit()
    data class Limited(val maxTransactionsCap: Int, val maxTransactionsLeft: Int) : TransactionsLimit()
}
