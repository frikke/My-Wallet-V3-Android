package com.blockchain.api.fraud.data

import kotlinx.serialization.Serializable

@Serializable
data class FraudFlowsResponse(
    val flows: List<FraudFlow>? = emptyList()
) {

    @Serializable
    data class FraudFlow(
        val name: String
    )
}
