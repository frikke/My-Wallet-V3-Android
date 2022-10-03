package com.blockchain.domain.paymentmethods.model

data class GooglePayInfo(
    val beneficiaryID: String,
    val merchantBankCountryCode: String,
    val googlePayParameters: String,
    val publishableApiKey: String,
    val allowPrepaidCards: Boolean?,
    val allowCreditCards: Boolean?,
    val allowedAuthMethods: List<String>?,
    val allowedCardNetworks: List<String>?,
    val billingAddressRequired: Boolean?,
    val billingAddressParameters: BillingAddressParameters
) {
    data class BillingAddressParameters(
        val format: String?,
        val phoneNumberRequired: Boolean?
    )
}
