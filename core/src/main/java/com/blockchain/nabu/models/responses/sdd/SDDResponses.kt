package com.blockchain.nabu.models.responses.sdd

import kotlinx.serialization.Serializable

@Serializable
data class SDDEligibilityResponse(
    val eligible: Boolean,
    val tier: Int
)

@Serializable
data class SDDStatusResponse(
    val verified: Boolean,
    val taskComplete: Boolean
)
