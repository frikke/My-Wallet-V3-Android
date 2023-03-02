package com.blockchain.api.announcements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementsDto(
    @SerialName("inAppMessages") val announcements: List<AnnouncementDto>
)

@Serializable
data class AnnouncementDto(
    @SerialName("messageId") val id: String,
    @SerialName("customPayload") val customPayload: AnnouncementPayloadDto,
    @SerialName("priorityLevel") val priorityLevel: Double,
    @SerialName("read") val isRead: Boolean,
    @SerialName("expiresAt") val expiresAt: Long,
)

@Serializable
data class AnnouncementPayloadDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("actionUrl") val actionUrl: String,
    @SerialName("appMode") val appMode: String
)
