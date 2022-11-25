package com.blockchain.earn.domain.models

import info.blockchain.balance.Money

data class StakingLimits(
    val minDepositValue: Money,
    val bondingDays: Int,
    val unbondingDays: Int,
    val withdrawalsDisabled: Boolean,
    val rewardsFrequency: EarnRewardsFrequency
)

enum class EarnRewardsFrequency {
    Daily,
    Weekly,
    Monthly,
    Unknown
}
