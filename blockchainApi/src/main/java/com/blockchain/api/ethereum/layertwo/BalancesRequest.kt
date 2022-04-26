package com.blockchain.api.ethereum.layertwo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BalancesRequest(
    @SerialName("address")
    val address: String,
    @SerialName("apiCode")
    val apiCode: String,
    @SerialName("network")
    val network: String
)
