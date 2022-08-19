package com.blockchain.payments.googlepay.manager.request

object GooglePayRequestBuilder {
    fun buildForPaymentStatus(
        allowedAuthMethods: List<String> = defaultAllowedAuthMethods,
        allowedCardNetworks: List<String> = defaultAllowedCardNetworks
    ): GooglePayRequest =
        GooglePayRequest(
            allowedPaymentMethods = listOf(
                CardPaymentMethod(
                    parameters = CardPaymentMethod.CardPaymentParameters(
                        allowedAuthMethods = allowedAuthMethods,
                        allowedCardNetworks = allowedCardNetworks,
                        billingAddressRequired = true,
                        billingAddressParameters = BillingAddressParameters(),
                        allowPrepaidCards = true,
                        allowCreditCards = false
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
        currencyCode: String,
        allowPrepaidCards: Boolean,
        allowCreditCards: Boolean,
        billingAddressRequired: Boolean,
        billingAddressParameters: BillingAddressParameters
    ): GooglePayRequest =
        GooglePayRequest(
            allowedPaymentMethods = listOf(
                CardPaymentMethod(
                    parameters = CardPaymentMethod.CardPaymentParameters(
                        allowedAuthMethods = allowedAuthMethods.ifInvalid { defaultAllowedAuthMethods },
                        allowedCardNetworks = allowedCardNetworks.ifInvalid { defaultAllowedCardNetworks },
                        billingAddressRequired = billingAddressRequired,
                        billingAddressParameters = billingAddressParameters.copy(
                            format = billingAddressParameters.format.ifEmpty { BillingAddressParameters().format }
                        ),
                        allowPrepaidCards = allowPrepaidCards,
                        allowCreditCards = allowCreditCards
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

    private fun List<String>.ifInvalid(defaultValue: () -> List<String>): List<String> {
        if (this.isEmpty() || this.filterNot { it.isEmpty() || it.isBlank() }.isEmpty()) {
            return defaultValue()
        }
        return this
    }
}

val defaultAllowedAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
val defaultAllowedCardNetworks: List<String> = listOf("AMEX", "MASTERCARD", "VISA")
