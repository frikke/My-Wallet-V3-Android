package com.blockchain.nabu.models.responses.nabu

import com.blockchain.serialization.JsonSerializable
import java.io.Serializable
import java.math.BigDecimal
import kotlinx.serialization.Contextual

@kotlinx.serialization.Serializable
data class TiersResponse(
    val tiers: List<TierResponse>
) : Serializable

@kotlinx.serialization.Serializable
data class TierResponse(
    val index: Int,
    val name: String,
    val state: KycTierState,
    val limits: LimitsJson? = null
) : JsonSerializable

@kotlinx.serialization.Serializable
data class LimitsJson(
    val currency: String,
    val daily: @Contextual BigDecimal? = null,
    val annual: @Contextual BigDecimal? = null
) : JsonSerializable
