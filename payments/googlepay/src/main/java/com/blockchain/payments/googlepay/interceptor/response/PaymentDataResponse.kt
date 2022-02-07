package com.blockchain.payments.googlepay.interceptor.response

import kotlinx.serialization.Serializable

@Serializable
data class PaymentDataResponse(
    val apiVersion: Int,
    val apiVersionMinor: Int,
    val paymentMethodData: PaymentMethodData,
    val shippingAddress: Address? = null
) {
    @Serializable
    data class PaymentMethodData(
        val description: String,
        val info: Info,
        val tokenizationData: TokenizationData,
        val type: String
    ) {
        @Serializable
        data class Info(
            val assuranceDetails: AssuranceDetails? = null,
            val billingAddress: Address? = null,
            val cardDetails: String,
            val cardNetwork: String
        ) {
            @Serializable
            data class AssuranceDetails(
                val accountVerified: Boolean,
                val cardHolderAuthenticated: Boolean
            )
        }

        @Serializable
        data class TokenizationData(
            val token: String,
            val type: String
        )
    }

    @Serializable
    data class Address(
        val address1: String,
        val address2: String,
        val address3: String,
        val administrativeArea: String,
        val countryCode: String,
        val locality: String,
        val name: String,
        val postalCode: String,
        val sortingCode: String
    )
}
