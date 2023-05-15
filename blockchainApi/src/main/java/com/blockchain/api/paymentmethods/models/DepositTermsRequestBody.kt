package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.Serializable

@Serializable
data class DepositTermsRequestBody(
    private val amount: Amount,
    private val paymentMethodId: String,
    private val product: String = "WALLET",
    private val purpose: String = "DEPOSIT"
) {
    @Serializable
    data class Amount(
        private val value: String,
        private val symbol: String
    )
}
