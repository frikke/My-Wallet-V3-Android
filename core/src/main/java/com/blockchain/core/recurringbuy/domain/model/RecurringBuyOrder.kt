package com.blockchain.core.recurringbuy.domain.model

data class RecurringBuyOrder(
    val state: RecurringBuyState = RecurringBuyState.UNINITIALISED,
    val id: String? = null,
)
