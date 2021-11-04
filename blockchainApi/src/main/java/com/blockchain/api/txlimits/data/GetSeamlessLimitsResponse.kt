package com.blockchain.api.txlimits.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LimitRange(
    @SerialName("limit")
    val limit: Limit,
    @SerialName("available")
    val available: Limit,
    @SerialName("used")
    val used: Limit
)

@Serializable
data class PeriodicLimit(
    @SerialName("limit")
    val limit: Limit,
    @SerialName("effective")
    val effective: Boolean? = null
)

@Serializable
data class CurrentLimits(
    @SerialName("available")
    val available: Limit,
    @SerialName("daily")
    val daily: PeriodicLimit? = null,
    @SerialName("monthly")
    val monthly: PeriodicLimit? = null,
    @SerialName("yearly")
    val yearly: PeriodicLimit? = null
)

@Serializable
data class SuggestedUpgrade(
    @SerialName("available")
    val available: Limit,
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
data class Limit(
    @SerialName("currency")
    val currency: String,
    @SerialName("value")
    val value: String // Minor value
)
