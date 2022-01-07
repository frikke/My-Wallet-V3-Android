package com.blockchain.api.txlimits.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetFeatureLimitsResponse(
    @SerialName("limits")
    val limits: List<FeatureLimitResponse>
)

@Serializable
data class FeatureLimitResponse(
    @SerialName("name")
    val name: String,
    @SerialName("enabled")
    val enabled: Boolean,
    @SerialName("limit")
    val limit: FeaturePeriodicLimit? = null
)

@Serializable
data class FeaturePeriodicLimit(
    @SerialName("value")
    val value: ApiMoneyMinor? = null,
    @SerialName("period")
    val period: String
)

enum class LimitPeriod {
    DAY,
    MONTH,
    YEAR
}

enum class FeatureName {
    SEND_CRYPTO,
    RECEIVE_CRYPTO,
    SWAP_CRYPTO,
    BUY_AND_SELL,
    BUY_WITH_CARD,
    BUY_AND_DEPOSIT_WITH_BANK,
    WITHDRAW_WITH_BANK,
    SAVINGS_INTEREST
}

@Serializable
data class ApiMoneyMinor(
    @SerialName("currency")
    val currency: String,
    @SerialName("value")
    val value: String
)
