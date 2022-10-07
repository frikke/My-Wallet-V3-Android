package com.blockchain.core.staking.domain.model

import info.blockchain.balance.Currency
import info.blockchain.balance.Money

data class StakingAccountBalance(
    val totalBalance: Money,
    val lockedBalance: Money,
    val pendingDeposit: Money,
    val pendingWithdrawal: Money
) {
    val availableBalance = totalBalance - lockedBalance

    companion object {
        fun zeroBalance(currency: Currency): StakingAccountBalance =
            StakingAccountBalance(
                totalBalance = Money.zero(currency),
                lockedBalance = Money.zero(currency),
                pendingDeposit = Money.zero(currency),
                pendingWithdrawal = Money.zero(currency)
            )
    }
}
