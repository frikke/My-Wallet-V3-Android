package com.blockchain.api.txlimits.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LimitRange(
    @SerialName("limit")
    val limit: Amount,
    @SerialName("available")
    val available: Amount,
    @SerialName("used")
    val used: Amount
)

@Serializable
data class Limit(
    @SerialName("limit")
    val limit: Amount,
    @SerialName("effective")
    val effective: Boolean? = null
)

@Serializable
data class CurrentLimits(
    @SerialName("available")
    val available: Amount,
    @SerialName("daily")
    val daily: Limit? = null,
    @SerialName("monthly")
    val monthly: Limit? = null,
    @SerialName("yearly")
    val yearly: Limit? = null
)

@Serializable
data class SuggestedUpgrade(
    @SerialName("available")
    val available: Amount,
    @SerialName("daily")
    val daily: LimitRange? = null,
    @SerialName("monthly")
    val monthly: LimitRange? = null,
    @SerialName("yearly")
    val yearly: LimitRange? = null,
    @SerialName("requiredTier")
    val requiredTier: Int,
    @SerialName("requirements")
    val requirements: List<String>
)

@Serializable
data class GetSeamlessLimitsResponse(
    @SerialName("currency")
    val currency: String,
    @SerialName("current")
    val current: CurrentLimits? = null,
    @SerialName("suggestedUpgrade")
    val suggestedUpgrade: SuggestedUpgrade? = null
)

@Serializable
data class Amount(
    @SerialName("currency")
    val currency: String,
    @SerialName("value")
    val value: String // Minor value
)