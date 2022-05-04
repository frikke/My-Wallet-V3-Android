package com.blockchain.api.nabu.data.contactpreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreferenceUpdate(
    @SerialName("contactMethod")
    val contactMethod: String,
    @SerialName("channel")
    val channel: String,
    @SerialName("action")
    val action: String // "ENABLE" or "DISABLE"
)
