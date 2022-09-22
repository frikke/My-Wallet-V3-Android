package com.blockchain.api.session.data

import kotlinx.serialization.Serializable

@Serializable
data class GenerateSessionResponse(
    val xSessionId: String
)
