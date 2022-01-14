package com.blockchain.api.paymentmethods.models

import com.blockchain.api.nabu.data.AddressRequest
import kotlinx.serialization.Serializable

@Serializable
data class AddNewCardBodyRequest(
    private val currency: String,
    private val address: AddressRequest,
    private val paymentMethodTokens: Map<String, String>?
)

@Serializable
data class AddNewCardResponse(
    val id: String,
    val partner: String
)

@Serializable
data class SimpleBuyConfirmationAttributes(
    private val everypay: EveryPayAttrs? = null,
    private val callback: String? = null,
    private val redirectURL: String?
)

@Serializable
data class EveryPayAttrs(private val customerUrl: String)
@Serializable
data class ActivateCardResponse(
    val everypay: EveryPayCardCredentialsResponse?,
    val cardProvider: CardProviderResponse?
)

@Serializable
data class EveryPayCardCredentialsResponse(
    val apiUsername: String,
    val mobileToken: String,
    val paymentLink: String
)

// cardAcquirerName and cardAcquirerAccountCode are mandatory
@Serializable
data class CardProviderResponse(
    val cardAcquirerName: String, // "STRIPE"
    val cardAcquirerAccountCode: String,
    val apiUserID: String?, // is the old apiUserName
    val apiToken: String?, // is the old mobile token and will be fill with bearer token most of the time
    val paymentLink: String?, // link should be followed background and if an action is required we should abort
    val paymentState: String?,
    val paymentReference: String?,
    val orderReference: String?,
    val clientSecret: String?, // use when client secret is needed (stripe)
    val publishableApiKey: String?
)
