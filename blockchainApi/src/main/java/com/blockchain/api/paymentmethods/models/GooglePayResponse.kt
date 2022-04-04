package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.Serializable

@Serializable
data class GooglePayResponse(
    val beneficiaryID: String,
    val merchantBankCountryCode: String,
    val googlePayParameters: String,
    val publishableApiKey: String,
    val allowPrepaidCards: Boolean? = true,
    val allowCreditCards: Boolean? = true
)
