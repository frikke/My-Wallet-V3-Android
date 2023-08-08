package com.blockchain.payments.vgs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VgsResponse(
    @SerialName("beneficiary_id")
    val beneficiaryId: String
)
