package com.blockchain.api.nabu.data.contactpreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactPreferencesResponse(
    @SerialName("language")
    val language: String,
    @SerialName("notificationMethods")
    val notificationMethods: List<NotificationMethod>,
    @SerialName("preferences")
    val preferences: List<Preference>
)
