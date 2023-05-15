package com.blockchain.api.referral.data

import com.blockchain.api.ActionData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReferralResponse(
    @SerialName("rewardTitle")
    val rewardTitle: String,
    @SerialName("rewardSubtitle")
    val rewardSubtitle: String,
    @SerialName("code")
    val code: String,
    @SerialName("campaignId")
    val campaignId: String,
    @SerialName("criteria")
    val criteria: List<String>,
    @SerialName("announcement")
    val announcement: StyleData?,
    @SerialName("promotion")
    val promotion: StyleData?
)

@Serializable
data class StyleData(
    val title: String,
    val message: String,
    val header: MediaInfo?,
    val icon: UrlInfo?,
    val style: StyleInfo?,
    val actions: List<ActionData>?
)

@Serializable
data class StyleInfo(
    val background: MediaInfo?,
    val foreground: ColorInfo?
)

@Serializable
data class MediaInfo(
    val media: UrlInfo?
)

@Serializable
data class UrlInfo(
    val url: String
)

@Serializable
data class ColorInfo(
    val color: HSBInfo?
)

@Serializable
data class HSBInfo(
    val hsb: List<Float>?
)
