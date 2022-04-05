package com.blockchain.payments.googlepay.manager.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GooglePayRequest(
    @SerialName("apiVersion")
    val apiVersion: Int = 2,
    @SerialName("apiVersionMinor")
    val apiVersionMinor: Int = 0,
    @SerialName("allowedPaymentMethods")
    val allowedPaymentMethods: List<CardPaymentMethod>,
    @SerialName("transactionInfo")
    val transactionInfo: TransactionInfo? = null,
    @SerialName("merchantInfo")
    val merchantInfo: MerchantInfo? = null,
    @SerialName("shippingAddressRequired")
    val shippingAddressRequired: Boolean? = null,
    @SerialName("shippingAddressParameters")
    val shippingAddressParameters: ShippingAddressParameters? = null
)

@Serializable
data class CardPaymentMethod(
    @SerialName("type")
    val type: String = "CARD",
    @SerialName("parameters")
    val parameters: CardPaymentParameters,
    @SerialName("tokenizationSpecification")
    val tokenizationSpecification: TokenizationSpecification? = null
) {
    @Serializable
    data class CardPaymentParameters(
        @SerialName("allowedAuthMethods")
        val allowedAuthMethods: List<String>,
        @SerialName("allowedCardNetworks")
        val allowedCardNetworks: List<String>,
        @SerialName("billingAddressRequired")
        val billingAddressRequired: Boolean = true,
        @SerialName("billingAddressParameters")
        val billingAddressParameters: BillingAddressParameters,
        @SerialName("allowPrepaidCards")
        val allowPrepaidCards: Boolean,
        @SerialName("allowCreditCards")
        val allowCreditCards: Boolean
    ) {
        @Serializable
        data class BillingAddressParameters(
            @SerialName("format")
            val format: String = "FULL"
        )
    }

    @Serializable
    data class TokenizationSpecification(
        @SerialName("type")
        val type: String = "PAYMENT_GATEWAY",
        @SerialName("parameters")
        val parameters: Map<String, String>
    )
}

@Serializable
data class TransactionInfo(
    @SerialName("totalPrice")
    val totalPrice: String,
    @SerialName("totalPriceStatus")
    val totalPriceStatus: String = "FINAL",
    @SerialName("countryCode")
    val countryCode: String,
    @SerialName("currencyCode")
    val currencyCode: String
)

@Serializable
data class MerchantInfo(
    @SerialName("merchantName")
    val merchantName: String
)

@Serializable
data class ShippingAddressParameters(
    @SerialName("phoneNumberRequired")
    val phoneNumberRequired: Boolean,
    @SerialName("allowedCountryCodes")
    val allowedCountryCodes: List<String>
)
