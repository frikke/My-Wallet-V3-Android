package com.blockchain.earn.domain.models

import info.blockchain.balance.Money

data class StakingLimits(
    val minDepositValue: Money,
    val bondingDays: Int,
    val unbondingDays: Int,
    val withdrawalsDisabled: Boolean
)
