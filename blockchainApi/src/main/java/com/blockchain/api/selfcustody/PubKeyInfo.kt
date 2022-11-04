package com.blockchain.api.selfcustody

import com.blockchain.domain.wallet.PubKeyStyle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PubKeyInfo(
    @SerialName("pubKey")
    val pubKey: String,
    @SerialName("style")
    val style: PubKeyStyle,
    @SerialName("descriptor")
    val descriptor: Int = 0
)
