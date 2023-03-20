package com.blockchain.api.announcements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AnnouncementBodyDto private constructor(
    @SerialName("email") val email: String,
    @SerialName("messageId") val messageId: String,
    @SerialName("deleteAction") val deleteAction: String?,
    @SerialName("clickedUrl") val clickedUrl: String?,
    @SerialName("deviceInfo") val deviceInfo: DeviceInfo,
) {
    companion object {
        fun consume(
            email: String,
            messageId: String,
            deleteAction: String,
            deviceInfo: DeviceInfo
        ) = AnnouncementBodyDto(
            email = email,
            messageId = messageId,
            deleteAction = deleteAction,
            clickedUrl = null,
            deviceInfo = deviceInfo
        )

        fun seen(
            email: String,
            messageId: String,
            deviceInfo: DeviceInfo
        ) = AnnouncementBodyDto(
            email = email,
            messageId = messageId,
            deleteAction = null,
            clickedUrl = null,
            deviceInfo = deviceInfo
        )

        fun click(
            email: String,
            messageId: String,
            clickedUrl: String,
            deviceInfo: DeviceInfo
        ) = AnnouncementBodyDto(
            email = email,
            messageId = messageId,
            deleteAction = null,
            clickedUrl = clickedUrl,
            deviceInfo = deviceInfo
        )
    }
}

@Serializable
data class DeviceInfo(
    @SerialName("appPackageName") val appPackageName: String,
    @SerialName("deviceId") val deviceId: String,
    @SerialName("platform") val platform: String
)
