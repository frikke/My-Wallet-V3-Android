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
        gateway: String,
        gatewayMerchantId: String,
        totalPrice: String,
        countryCode: String,
        currencyCode: String,
        merchantName: String
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
                        parameters = CardPaymentMethod.TokenizationSpecification.Parameters(
                            gateway = gateway,
                            gatewayMerchantId = gatewayMerchantId
                        )
                    )
                )
            ),
            transactionInfo = TransactionInfo(
                totalPrice = totalPrice,
                countryCode = countryCode,
                currencyCode = currencyCode
            ),
            merchantInfo = MerchantInfo(
                merchantName = merchantName
            ),
            shippingAddressRequired = false
        )
}

val allowedAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
val allowedCardNetworks: List<String> = listOf("AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA")
