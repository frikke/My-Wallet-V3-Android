package com.blockchain.api.kyc.model

import java.math.BigDecimal
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class KycTiersDto(
    val tiers: List<KycTierDto>
)

@Serializable
data class KycTierDto(
    val index: Int,
    val name: String,
    val state: String,
    val limits: KycLimitsDto? = null
)

@Serializable
data class KycLimitsDto(
    val currency: String,
    val daily: @Contextual BigDecimal? = null,
    val annual: @Contextual BigDecimal? = null
)
