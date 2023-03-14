package com.blockchain.api.announcements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsumeAnnouncementDto(
    @SerialName("email") val email: String,
    @SerialName("messageId") val messageId: String,
    @SerialName("deleteAction") val deleteAction: String,
    @SerialName("deviceInfo") val deviceInfo: DeviceInfo,
)

@Serializable
data class DeviceInfo(
    @SerialName("appPackageName") val appPackageName: String,
    @SerialName("deviceId") val deviceId: String,
    @SerialName("platform") val platform: String
)
