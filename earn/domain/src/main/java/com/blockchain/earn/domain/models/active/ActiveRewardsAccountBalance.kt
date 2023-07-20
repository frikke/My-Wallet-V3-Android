package com.blockchain.earn.domain.models.active

import com.blockchain.earn.domain.models.EarnAccountBalance
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

data class ActiveRewardsAccountBalance(
    override val totalBalance: Money,
    val lockedBalance: Money,
    val pendingDeposit: Money,
    val pendingWithdrawal: Money,
    val totalRewards: Money,
    val earningBalance: Money,
    val bondingDeposits: Money
) : EarnAccountBalance {
    val availableBalance = totalBalance - lockedBalance

    companion object {
        fun zeroBalance(currency: Currency): ActiveRewardsAccountBalance =
            ActiveRewardsAccountBalance(
                totalBalance = Money.zero(currency),
                lockedBalance = Money.zero(currency),
                pendingDeposit = Money.zero(currency),
                pendingWithdrawal = Money.zero(currency),
                totalRewards = Money.zero(currency),
                earningBalance = Money.zero(currency),
                bondingDeposits = Money.zero(currency)
            )
    }
}
