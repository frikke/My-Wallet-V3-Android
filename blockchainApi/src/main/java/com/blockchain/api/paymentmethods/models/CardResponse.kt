package com.blockchain.api.paymentmethods.models

import com.blockchain.api.NabuUxErrorResponse
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
    val partner: String,
    @SerialName("ux")
    val ux: NabuUxErrorResponse? = null
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
    private val isMitPayment: Boolean? = false,
    @SerialName("googlePayPayload")
    private val googlePayPayload: String? = null,
    @SerialName("cvv")
    private val cvv: String? = null,
    @SerialName("isAsync")
    val isAsync: Boolean? = null,
    @SerialName("paymentContact")
    private val paymentContact: PaymentContact? = null
)

@Serializable
data class PaymentContact(
    @SerialName("line1")
    private val line1: String? = "",
    @SerialName("line2")
    private val line2: String? = "",
    @SerialName("city")
    private val city: String? = "",
    @SerialName("state")
    private val state: String? = "",
    @SerialName("country")
    private val country: String? = "",
    @SerialName("postCode")
    private val postCode: String? = "",
    @SerialName("firstname")
    private val firstname: String? = "",
    @SerialName("lastname")
    private val lastname: String? = "",
    @SerialName("phone")
    private val phone: String? = ""
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
    val cardProvider: CardProviderResponse?,
    @SerialName("ux")
    val ux: NabuUxErrorResponse? = null
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

@Serializable
data class CardRejectionStateResponse(
    val block: Boolean,
    val ux: NabuUxErrorResponse?
)
