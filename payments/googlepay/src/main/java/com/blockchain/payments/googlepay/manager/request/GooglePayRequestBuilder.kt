package com.blockchain.payments.googlepay.manager.request

object GooglePayRequestBuilder {
    fun buildForPaymentStatus(allowedAuthMethods: List<String>, allowedCardNetworks: List<String>): GooglePayRequest =
        GooglePayRequest(
            allowedPaymentMethods = listOf(
                CardPaymentMethod(
                    parameters = CardPaymentMethod.CardPaymentParameters(
                        allowedAuthMethods = allowedAuthMethods,
                        allowedCardNetworks = allowedCardNetworks,
                        billingAddressRequired = true,
                        billingAddressParameters = CardPaymentMethod.CardPaymentParameters.BillingAddressParameters()
                    )
                )
            )
        )

    fun buildForPaymentRequest(
        allowedAuthMethods: List<String>,
        allowedCardNetworks: List<String>,
        gatewayTokenizationParameters: Map<String, String>,
        totalPrice: String,
        countryCode: String,
        currencyCode: String
    ): GooglePayRequest =
        GooglePayRequest(
            allowedPaymentMethods = listOf(
                CardPaymentMethod(
                    parameters = CardPaymentMethod.CardPaymentParameters(
                        allowedAuthMethods = allowedAuthMethods,
                        allowedCardNetworks = allowedCardNetworks,
                        billingAddressRequired = false,
                        billingAddressParameters = CardPaymentMethod.CardPaymentParameters.BillingAddressParameters()
                    ),
                    tokenizationSpecification = CardPaymentMethod.TokenizationSpecification(
                        parameters = gatewayTokenizationParameters
                    )
                )
            ),
            transactionInfo = TransactionInfo(
                totalPrice = totalPrice,
                countryCode = countryCode,
                currencyCode = currencyCode
            ),
            merchantInfo = MerchantInfo(
                merchantName = "Blockchain.com"
            ),
            shippingAddressRequired = false
        )
}

val allowedAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
val allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA")
