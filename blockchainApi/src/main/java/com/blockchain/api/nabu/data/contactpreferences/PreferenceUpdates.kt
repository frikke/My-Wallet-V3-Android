package com.blockchain.api.nabu.data.contactpreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreferenceUpdates(
    @SerialName("preferences")
    val preferences: List<PreferenceUpdate>
)
