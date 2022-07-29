package com.blockchain.api.interest.data

import kotlinx.serialization.Serializable

@Serializable
data class InterestTickerLimitsDto(
    val tickerLimits: Map<String, InterestLimitsDto>
)

@Serializable
data class InterestLimitsDto(
    val currency: String,
    val lockUpDuration: Int,
    val maxWithdrawalAmount: String,
    val minDepositAmount: String
)