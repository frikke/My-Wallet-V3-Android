package com.blockchain.core.buy.domain.models

data class SimpleBuyEligibility(
    val eligible: Boolean,
    val simpleBuyTradingEligible: Boolean,
    val pendingDepositSimpleBuyTrades: Int,
    val maxPendingDepositSimpleBuyTrades: Int
) {
    val isPendingDepositThresholdReached: Boolean
        get() = pendingDepositSimpleBuyTrades >= maxPendingDepositSimpleBuyTrades
}