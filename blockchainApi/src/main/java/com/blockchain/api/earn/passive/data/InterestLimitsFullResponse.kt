package com.blockchain.api.earn.passive.data

import kotlinx.serialization.Serializable

@Serializable
data class InterestTickerLimitsDto(
    val limits: Map<String, InterestLimitsDto>
)

@Serializable
data class InterestLimitsDto(
    val currency: String,
    val lockUpDuration: Int,
    val maxWithdrawalAmount: String,
    val minDepositAmount: String
)
