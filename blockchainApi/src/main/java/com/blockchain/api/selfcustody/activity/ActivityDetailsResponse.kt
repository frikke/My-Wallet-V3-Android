package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityDetailsResponse(
    @SerialName("type")
    val type: String,
    @SerialName("")
    val detail: ActivityDetailGroupsDto
)
