package com.blockchain.domain.eligibility.model

import java.io.Serializable

@kotlinx.serialization.Serializable
sealed class TransactionsLimit : Serializable {
    @kotlinx.serialization.Serializable
    object Unlimited : TransactionsLimit()

    @kotlinx.serialization.Serializable
    data class Limited(val maxTransactionsCap: Int, val maxTransactionsLeft: Int) : TransactionsLimit()
}
