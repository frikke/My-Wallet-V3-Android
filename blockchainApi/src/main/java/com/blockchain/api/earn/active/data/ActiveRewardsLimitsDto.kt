package com.blockchain.api.earn.active.data

import kotlinx.serialization.Serializable

@Serializable
data class ActiveRewardsLimitsMapDto(
    val limits: Map<String, ActiveRewardsLimitsDto>
)

@Serializable
data class ActiveRewardsLimitsDto(
    val minDepositValue: String,
    val bondingDays: Int,
    val unbondingDays: Int?,
    val disabledWithdrawals: Boolean?,
    val rewardFrequency: String
)
