package com.blockchain.earn.domain.models.staking

import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class StakingLimits(
    val minDepositValue: Money,
    val bondingDays: Int,
    val unbondingDays: Int,
    val withdrawalsDisabled: Boolean,
    val rewardsFrequency: EarnRewardsFrequency
)
