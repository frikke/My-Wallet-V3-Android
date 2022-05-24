package com.blockchain.domain.paymentmethods.model

data class GooglePayInfo(
    val beneficiaryID: String,
    val merchantBankCountryCode: String,
    val googlePayParameters: String,
    val publishableApiKey: String,
    val allowPrepaidCards: Boolean?,
    val allowCreditCards: Boolean?
)
