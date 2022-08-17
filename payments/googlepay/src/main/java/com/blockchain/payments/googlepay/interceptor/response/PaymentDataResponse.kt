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
    val shippingAddress: Address? = null,
    @SerialName("billingAddress")
    val billingAddress: Address? = null
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
        val address1: String? = null,
        @SerialName("address2")
        val address2: String? = null,
        @SerialName("address3")
        val address3: String? = null,
        @SerialName("administrativeArea")
        val administrativeArea: String? = null,
        @SerialName("countryCode")
        val countryCode: String? = null,
        @SerialName("locality")
        val locality: String? = null,
        @SerialName("name")
        val name: String? = null,
        @SerialName("postalCode")
        val postalCode: String? = null,
        @SerialName("sortingCode")
        val sortingCode: String? = null
    )
}
