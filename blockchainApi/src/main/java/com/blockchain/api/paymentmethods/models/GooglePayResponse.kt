package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.Serializable

@Serializable
data class GooglePayResponse(
    val beneficiaryID: String,
    val merchantBankCountryCode: String,
    val googlePayParameters: String,
    val publishableApiKey: String,
    val allowPrepaidCards: Boolean? = false,
    val allowCreditCards: Boolean? = false,
    val allowedAuthMethods: List<String>?,
    val billingAddressRequired: Boolean? = true,
    val billingAddressParameters: BillingAddressParameters?,
    val allowedCardNetworks: List<String>?
) {
    @Serializable
    data class BillingAddressParameters(
        val format: String = "FULL",
        val phoneNumberRequired: Boolean = false
    )
}
