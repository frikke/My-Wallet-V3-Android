package com.blockchain.api.referral.data

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
    @SerialName("criteria")
    val criteria: List<String>
)
