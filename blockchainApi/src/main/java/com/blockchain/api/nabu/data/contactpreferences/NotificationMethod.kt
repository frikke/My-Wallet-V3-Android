package com.blockchain.api.nabu.data.contactpreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationMethod(
    @SerialName("method")
    val method: String,
    @SerialName("title")
    val title: String,
    @SerialName("configured")
    val configured: Boolean,
    @SerialName("verified")
    val verified: Boolean
)
