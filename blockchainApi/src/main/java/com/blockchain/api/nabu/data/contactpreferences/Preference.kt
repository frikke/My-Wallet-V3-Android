package com.blockchain.api.nabu.data.contactpreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Preference(
    @SerialName("type")
    val type: String,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String,
    @SerialName("description")
    val description: String,
    @SerialName("enabledMethods")
    val enabledMethods: List<String>,
    @SerialName("optionalMethods")
    val optionalMethods: List<String>,
    @SerialName("requiredMethods")
    val requiredMethods: List<String>

)
