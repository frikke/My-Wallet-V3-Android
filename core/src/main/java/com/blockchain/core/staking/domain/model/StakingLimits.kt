package com.blockchain.core.staking.domain.model

import info.blockchain.balance.Money

data class StakingLimits(
    val minDepositValue: Money,
    val bondingDays: Int,
    val unbondingDays: Int,
    val withdrawalsDisabled: Boolean
)
