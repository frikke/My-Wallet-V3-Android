package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
data class SendWithdrawalAddressesRequest(
    val addresses: Map<String, String>
)
