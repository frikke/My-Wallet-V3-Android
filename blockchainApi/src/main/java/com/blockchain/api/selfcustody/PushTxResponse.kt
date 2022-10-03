package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PushTxResponse(
    @SerialName("txId")
    val txId: String
)
