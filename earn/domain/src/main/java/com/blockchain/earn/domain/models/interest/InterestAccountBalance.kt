package com.blockchain.earn.domain.models.interest

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money

data class InterestAccountBalance(
    val totalBalance: Money,
    val pendingInterest: Money,
    val pendingDeposit: Money,
    val totalInterest: Money,
    val lockedBalance: Money,
    val dashboardDisplay: Money,
    val hasTransactions: Boolean = false,
) {
    val actionableBalance: CryptoValue
        get() = (totalBalance - lockedBalance) as CryptoValue
}
