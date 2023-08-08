package com.blockchain.earn.domain.models

import info.blockchain.balance.Money

data class StakingRewardsRates(
    val rate: Double,
    val commission: Double
)

data class ActiveRewardsRates(
    val rate: Double,
    val commission: Double,
    val triggerPrice: Money
)
