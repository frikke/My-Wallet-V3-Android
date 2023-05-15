package com.blockchain.api.kyc.model

import kotlinx.serialization.Serializable

@Serializable
data class KycFlowResponse(
    val nextFlow: String
)
