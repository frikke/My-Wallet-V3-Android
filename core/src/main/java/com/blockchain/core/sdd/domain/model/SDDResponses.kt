package com.blockchain.core.sdd.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SddEligibilityDto(
    val eligible: Boolean,
    val tier: Int
)

@Serializable
data class SddStatusDto(
    val verified: Boolean,
    val taskComplete: Boolean
)
