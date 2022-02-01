package com.blockchain.api.paymentmethods.models

import com.blockchain.api.nabu.data.AddressRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddNewCardBodyRequest(
    @SerialName("currency")
    private val currency: String,
    @SerialName("address")
    private val address: AddressRequest,
    @SerialName("paymentMethodTokens")
    private val paymentMethodTokens: Map<String, String>?
)

@Serializable
data class AddNewCardResponse(
    @SerialName("id")
    val id: String,
    @SerialName("partner")
    val partner: String
)

@Serializable
data class SimpleBuyConfirmationAttributes(
    @SerialName("everypay")
    private val everypay: EveryPayAttrs? = null,
    @SerialName("callback")
    private val callback: String? = null,
    @SerialName("redirectURL")
    private val redirectURL: String?,
    @SerialName("disable3DS")
    private val disable3DS: Boolean? = null,
    @SerialName("isMitPayment")
    private val isMitPayment: Boolean? = null,
    @SerialName("googlePayPayload")
    private val googlePayPayload: String? = null
)

@Serializable
data class EveryPayAttrs(
    @SerialName("customerUrl")
    private val customerUrl: String
)
@Serializable
data class ActivateCardResponse(
    @SerialName("everypay")
    val everypay: EveryPayCardCredentialsResponse?,
    @SerialName("cardProvider")
    val cardProvider: CardProviderResponse?
)

@Serializable
data class EveryPayCardCredentialsResponse(
    @SerialName("apiUsername")
    val apiUsername: String,
    @SerialName("mobileToken")
    val mobileToken: String,
    @SerialName("paymentLink")
    val paymentLink: String
)

// cardAcquirerName and cardAcquirerAccountCode are mandatory
@Serializable
data class CardProviderResponse(
    @SerialName("cardAcquirerName")
    val cardAcquirerName: String, // "STRIPE"
    @SerialName("cardAcquirerAccountCode")
    val cardAcquirerAccountCode: String,
    @SerialName("apiUserID")
    val apiUserID: String?, // is the old apiUserName
    @SerialName("apiToken")
    val apiToken: String?, // is the old mobile token and will be fill with bearer token most of the time
    @SerialName("paymentLink")
    val paymentLink: String?, // link should be followed background and if an action is required we should abort
    @SerialName("paymentState")
    val paymentState: String?,
    @SerialName("paymentReference")
    val paymentReference: String?,
    @SerialName("orderReference")
    val orderReference: String?,
    @SerialName("clientSecret")
    val clientSecret: String?, // use when client secret is needed (stripe)
    @SerialName("publishableApiKey")
    val publishableApiKey: String?
)
