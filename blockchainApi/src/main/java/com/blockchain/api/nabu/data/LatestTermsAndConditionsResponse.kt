package com.blockchain.api.nabu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatestTermsAndConditionsResponse(
    @SerialName("termsAndConditions")
    val termsAndConditionsUrl: String?
)
