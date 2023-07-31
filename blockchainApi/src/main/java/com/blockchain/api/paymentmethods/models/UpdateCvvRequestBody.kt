package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCvvRequestBody(
    val paymentId: String,
    val cvv: String
)
