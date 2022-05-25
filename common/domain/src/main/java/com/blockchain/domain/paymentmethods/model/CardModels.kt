package com.blockchain.domain.paymentmethods.model

data class CardToBeActivated(val partner: Partner, val cardId: String)

enum class Partner {
    EVERYPAY, // The old flow with EveryPay
    CARDPROVIDER, // The new flow for CardAcquirers, which can be Checkout, Stripe and EveryPay
    UNKNOWN
}

// Very similar to CardAttributes.Provider, used for card activation
data class CardProvider(
    val cardAcquirerName: String,
    val cardAcquirerAccountCode: String,
    val apiUserID: String,
    val apiToken: String,
    val paymentLink: String,
    val paymentState: String,
    val paymentReference: String,
    val orderReference: String,
    val clientSecret: String,
    val publishableApiKey: String
)

data class EveryPayCredentials(
    val apiUsername: String,
    val mobileToken: String,
    val paymentLink: String
)

sealed class PartnerCredentials {
    object Unknown : PartnerCredentials()
    data class EverypayPartner(val everyPay: EveryPayCredentials) : PartnerCredentials()
    data class CardProviderPartner(val cardProvider: CardProvider) : PartnerCredentials()
}
