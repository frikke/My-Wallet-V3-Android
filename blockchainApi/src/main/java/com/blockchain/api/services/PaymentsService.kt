package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.BANK_ACCOUNT
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.BANK_TRANSFER
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.PAYMENT_CARD
import com.blockchain.api.payments.data.WithdrawalLocksResponse
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import io.reactivex.rxjava3.core.Single

class PaymentsService internal constructor(
    private val api: PaymentsApi
) {
    suspend fun getPaymentMethodDetailsForId(
        authHeader: String,
        paymentId: String
    ): Outcome<ApiError, PaymentMethodDetails> =
        api.getPaymentMethodDetailsForId(authHeader, paymentId)
            .map { it.toPaymentDetails() }

    fun getWithdrawalLocks(
        authHeader: String,
        localCurrency: String
    ): Single<CollateralLocks> =
        api.getWithdrawalLocks(authHeader, localCurrency)
            .map { it.toWithdrawalLocks() }
}

private fun PaymentMethodDetailsResponse.toPaymentDetails(): PaymentMethodDetails {
    return when (this.paymentMethodType) {
        PAYMENT_CARD -> {
            PaymentMethodDetails(
                label = cardDetails?.card?.label,
                endDigits = cardDetails?.card?.number
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

private fun WithdrawalLocksResponse.toWithdrawalLocks() =
    CollateralLocks(
        currency = this.totalLocked.currency,
        value = this.totalLocked.amount,
        locks = this.locks.map { lockPeriod ->
            CollateralLock(
                currency = lockPeriod.localCurrencyAmount.currency,
                value = lockPeriod.localCurrencyAmount.amount,
                date = lockPeriod.expiresAt
            )
        }
    )

data class CollateralLocks(
    val currency: String,
    val value: String,
    val locks: List<CollateralLock>
)

data class CollateralLock(
    val currency: String,
    val value: String,
    val date: String
)
