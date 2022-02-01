package com.blockchain.payments.googlepay.manager.request

import kotlinx.serialization.Serializable

@Serializable
data class GooglePayRequest(
    val apiVersion: Int = 2,
    val apiVersionMinor: Int = 0,
    val allowedPaymentMethods: List<CardPaymentMethod>,
    val transactionInfo: TransactionInfo? = null,
    val merchantInfo: MerchantInfo? = null,
    val shippingAddressRequired: Boolean? = null,
    val shippingAddressParameters: ShippingAddressParameters? = null
)

@Serializable
data class CardPaymentMethod(
    val type: String = "CARD",
    val parameters: CardPaymentParameters,
    val tokenizationSpecification: TokenizationSpecification? = null
) {
    @Serializable
    data class CardPaymentParameters(
        val allowedAuthMethods: List<String>,
        val allowedCardNetworks: List<String>,
        val billingAddressRequired: Boolean = true,
        val billingAddressParameters: BillingAddressParameters
    ) {
        @Serializable
        data class BillingAddressParameters(
            val format: String = "FULL"
        )
    }

    @Serializable
    data class TokenizationSpecification(
        val type: String = "PAYMENT_GATEWAY",
        val parameters: Map<String, String>
    )
}

@Serializable
data class TransactionInfo(
    val totalPrice: String,
    val totalPriceStatus: String = "FINAL",
    val countryCode: String,
    val currencyCode: String
)

@Serializable
data class MerchantInfo(
    val merchantName: String
)

@Serializable
data class ShippingAddressParameters(
    val phoneNumberRequired: Boolean,
    val allowedCountryCodes: List<String>
)
