package com.blockchain.api.referral.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReferralCode(
    @SerialName("referralCode")
    private val referralCode: String
)
