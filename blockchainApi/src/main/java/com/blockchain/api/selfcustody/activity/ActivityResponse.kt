package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityResponse(
    @SerialName("activity")
    val activity: List<ActivityItemDto>,
    @SerialName("nextPage")
    val nextPage: String?
)

@Serializable
data class ActivityItemDto(
    @SerialName("id")
    val id: String,
    @SerialName("externalUrl")
    val externalUrl: String,
    @SerialName("item")
    val summary: ActivityViewItemDto,
    @SerialName("detail")
    val detail: ActivityDetailGroupsDto,
    @SerialName("state")
    val status: String,
    @SerialName("timestamp")
    val timestamp: Long
)
