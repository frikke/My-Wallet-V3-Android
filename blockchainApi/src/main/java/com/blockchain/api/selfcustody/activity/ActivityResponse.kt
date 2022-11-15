package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityResponse(
    @SerialName("seqnum")
    val seqnum: Int,
    @SerialName("event")
    val event: String,
    @SerialName("channel")
    val channel: String,
    @SerialName("data")
    val activityData: NetworkActivityResponse
)

@Serializable
data class NetworkActivityResponse(
    @SerialName("network")
    val network: String,
    @SerialName("pubKey")
    val pubKey: String,
    @SerialName("activity")
    val activity: List<ActivityItemDto>
)

@Serializable
data class ActivityItemDto(
    @SerialName("id")
    val id: String,
    @SerialName("externalUrl")
    val externalUrl: String,
    @SerialName("item")
    val summary: ActivityViewItemDto,
    @SerialName("state")
    val status: String,
    @SerialName("timestamp")
    val timestamp: Long?
)
