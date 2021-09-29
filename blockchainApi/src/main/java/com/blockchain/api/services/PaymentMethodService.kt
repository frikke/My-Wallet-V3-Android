package com.blockchain.api.services

import com.blockchain.api.paymentMethods.PaymentMethodsApi
import com.blockchain.api.paymentMethods.data.PaymentMethodDetailsResponse
import com.blockchain.api.paymentMethods.data.PaymentMethodDetailsResponse.Companion.BANK_ACCOUNT
import com.blockchain.api.paymentMethods.data.PaymentMethodDetailsResponse.Companion.BANK_TRANSFER
import com.blockchain.api.paymentMethods.data.PaymentMethodDetailsResponse.Companion.PAYMENT_CARD
import io.reactivex.rxjava3.core.Single

class PaymentMethodService internal constructor(
    private val api: PaymentMethodsApi
) {
    fun getPaymentMethodDetailsForId(
        authHeader: String,
        paymentId: String
    ): Single<PaymentMethodDetails> =
        api.getPaymentMethodDetailsForId(authHeader, paymentId)
            .map { it.toPaymentDetails() }
}

private fun PaymentMethodDetailsResponse.toPaymentDetails(): PaymentMethodDetails {
    return when (this.paymentMethodType) {
        PAYMENT_CARD -> {
            check(this.cardDetails != null) { "CardDetails not present" }
            check(this.cardDetails.card != null) { "Card not present" }
            PaymentMethodDetails(
                label = cardDetails.card.label,
                endDigits = cardDetails.card.number
            )
        }
        BANK_TRANSFER -> {
            check(this.bankTransferAccountDetails != null) { "bankTransferAccountDetails not present" }
            check(this.bankTransferAccountDetails.details != null) { "bankTransferAccountDetails not present" }
            PaymentMethodDetails(
                label = bankTransferAccountDetails.details.accountName,
                endDigits = bankTransferAccountDetails.details.accountNumber
            )
        }
        BANK_ACCOUNT -> {
            check(this.bankAccountDetails != null) { "bankAccountDetails not present" }
            check(this.bankAccountDetails.extraAttributes != null) { "extraAttributes not present" }
            PaymentMethodDetails(
                label = bankAccountDetails.extraAttributes.name,
                endDigits = bankAccountDetails.extraAttributes.address
            )
        }
        else -> PaymentMethodDetails()
    }
}

data class PaymentMethodDetails(
    val label: String? = null,
    val endDigits: String? = null
)
