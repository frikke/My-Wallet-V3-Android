package com.blockchain.earn.domain.models.active

import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class ActiveRewardsLimits(
    val minDepositValue: Money,
    val bondingDays: Int,
    val unbondingDays: Int,
    val withdrawalsDisabled: Boolean,
    val rewardsFrequency: EarnRewardsFrequency
)
