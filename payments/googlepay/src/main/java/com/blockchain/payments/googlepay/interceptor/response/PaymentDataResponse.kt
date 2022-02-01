package com.blockchain.payments.googlepay.interceptor.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentDataResponse(
    @SerialName("apiVersion")
    val apiVersion: Int,
    @SerialName("apiVersionMinor")
    val apiVersionMinor: Int,
    @SerialName("paymentMethodData")
    val paymentMethodData: PaymentMethodData,
    @SerialName("shippingAddress")
    val shippingAddress: Address? = null
) {
    @Serializable
    data class PaymentMethodData(
        @SerialName("description")
        val description: String,
        @SerialName("info")
        val info: Info,
        @SerialName("tokenizationData")
        val tokenizationData: TokenizationData,
        @SerialName("type")
        val type: String
    ) {
        @Serializable
        data class Info(
            @SerialName("assuranceDetails")
            val assuranceDetails: AssuranceDetails? = null,
            @SerialName("billingAddress")
            val billingAddress: Address? = null,
            @SerialName("cardDetails")
            val cardDetails: String,
            @SerialName("cardNetwork")
            val cardNetwork: String
        ) {
            @Serializable
            data class AssuranceDetails(
                @SerialName("accountVerified")
                val accountVerified: Boolean,
                @SerialName("cardHolderAuthenticated")
                val cardHolderAuthenticated: Boolean
            )
        }

        @Serializable
        data class TokenizationData(
            @SerialName("token")
            val token: String,
            @SerialName("type")
            val type: String
        )
    }

    @Serializable
    data class Address(
        @SerialName("address1")
        val address1: String,
        @SerialName("address2")
        val address2: String,
        @SerialName("address3")
        val address3: String,
        @SerialName("administrativeArea")
        val administrativeArea: String,
        @SerialName("countryCode")
        val countryCode: String,
        @SerialName("locality")
        val locality: String,
        @SerialName("name")
        val name: String,
        @SerialName("postalCode")
        val postalCode: String,
        @SerialName("sortingCode")
        val sortingCode: String
    )
}
