package com.blockchain.api.services

import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.BANK_ACCOUNT
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.BANK_TRANSFER
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.PAYMENT_CARD
import com.blockchain.api.payments.data.WithdrawalLocksResponse
import io.reactivex.rxjava3.core.Single

class PaymentsService internal constructor(
    private val api: PaymentsApi
) {
    fun getPaymentMethodDetailsForId(
        authHeader: String,
        paymentId: String
    ): Single<PaymentMethodDetails> =
        api.getPaymentMethodDetailsForId(authHeader, paymentId)
            .map { it.toPaymentDetails() }

    fun getWithdrawalLocks(
        authHeader: String,
        localCurrency: String
    ): Single<WithdrawalsApi> =
        api.getWithdrawalLocks(authHeader, localCurrency)
            .map { it.toWithdrawalLocks() }
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

private fun WithdrawalLocksResponse.toWithdrawalLocks() =
    WithdrawalsApi(
        totalAmount =
        LocalAmountApi(
            currency = this.totalLocked.currency,
            value = this.totalLocked.amount
        ),
        locks = this.locks.map { lockPeriod ->
            WithdrawalLockApi(
                amount = LocalAmountApi(
                    currency = lockPeriod.localCurrencyAmount.currency,
                    value = lockPeriod.localCurrencyAmount.amount
                ),
                date = lockPeriod.expiresAt
            )
        }
    )

data class WithdrawalsApi(
    val totalAmount: LocalAmountApi,
    val locks: List<WithdrawalLockApi>
)

data class WithdrawalLockApi(
    val amount: LocalAmountApi,
    val date: String
)

data class LocalAmountApi(
    val currency: String,
    val value: String
)