package com.blockchain.earn.domain.models.staking

import com.blockchain.earn.domain.models.EarnAccountBalance
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

data class StakingAccountBalance(
    override val totalBalance: Money,
    val lockedBalance: Money,
    val pendingDeposit: Money,
    val pendingWithdrawal: Money,
    val totalRewards: Money
) : EarnAccountBalance {
    val availableBalance = totalBalance - lockedBalance - pendingWithdrawal - pendingDeposit

    companion object {
        fun zeroBalance(currency: Currency): StakingAccountBalance =
            StakingAccountBalance(
                totalBalance = Money.zero(currency),
                lockedBalance = Money.zero(currency),
                pendingDeposit = Money.zero(currency),
                pendingWithdrawal = Money.zero(currency),
                totalRewards = Money.zero(currency)
            )
    }
}
