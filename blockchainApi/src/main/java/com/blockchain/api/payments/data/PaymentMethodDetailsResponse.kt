package com.blockchain.api.payments.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodDetailsResponse(
    @SerialName("paymentMethodType")
    val paymentMethodType: String,
    @SerialName("cardDetails")
    val cardDetails: CardResponse? = null,
    @SerialName("bankTransferAccountDetails")
    val bankTransferAccountDetails: LinkedBankTransferResponse? = null,
    @SerialName("bankAccountDetails")
    val bankAccountDetails: PaymentAccountResponse? = null
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val BANK_ACCOUNT = "BANK_ACCOUNT"
        const val FUNDS = "FUNDS"
    }
}

// PAYMENT_CARD

@Serializable
data class CardResponse(
    @SerialName("card")
    val card: CardDetailsResponse?,
    @SerialName("mobilePaymentType")
    val mobilePaymentType: String? = null
)

@Serializable
data class CardDetailsResponse(
    @SerialName("number")
    val number: String,
    @SerialName("type")
    val type: String,
    @SerialName("label")
    val label: String?
)

// BANK_TRANSFER

@Serializable
data class LinkedBankTransferResponse(
    @SerialName("details")
    val details: LinkedBankDetailsResponse?
)

@Serializable
data class LinkedBankDetailsResponse(
    @SerialName("accountNumber")
    val accountNumber: String,
    @SerialName("bankName")
    val bankName: String?,
    @SerialName("accountName")
    val accountName: String?
)

// BANK_ACCOUNT

@Serializable
data class PaymentAccountResponse(
    @SerialName("extraAttributes")
    val extraAttributes: ExtraAttributes? = null
)

@Serializable
data class ExtraAttributes(
    @SerialName("name")
    val name: String?,
    @SerialName("type")
    val type: String?,
    @SerialName("address")
    val address: String?
)
