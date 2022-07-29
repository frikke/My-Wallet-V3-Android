package com.blockchain.nabu.models.responses.interest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterestRateResponse(
    val rate: Double
)

@Serializable
data class InterestAddressResponse(
    @SerialName("accountRef") val address: String
)

@Serializable
data class InterestLimitsFullResponse(
    val limits: Map<String, InterestLimitsResponse>
)

@Serializable
data class InterestLimitsResponse(
    val currency: String,
    val lockUpDuration: Int,
    val maxWithdrawalAmount: String,
    val minDepositAmount: String
)

@Serializable
data class InterestWithdrawalBody(
    val withdrawalAddress: String,
    val amount: String,
    val currency: String
)
