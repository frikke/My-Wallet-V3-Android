package com.blockchain.api.mercuryexperiments.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MercuryExperimentsResponse(
    @SerialName("walletAwarenessPrompt")
    val walletAwarenessPrompt: Int?
)
