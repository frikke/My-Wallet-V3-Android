package com.blockchain.api.services

import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.BANK_ACCOUNT
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.BANK_TRANSFER
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse.Companion.PAYMENT_CARD
import com.blockchain.api.payments.data.WithdrawalLocksResponse
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import io.reactivex.rxjava3.core.Single

class PaymentsService internal constructor(
    private val api: PaymentsApi
) {
    suspend fun getPaymentMethodDetailsForId(
        paymentId: String
    ): Outcome<Exception, PaymentMethodDetailsResponse> =
        api.getPaymentMethodDetailsForId(paymentId)

    fun getWithdrawalLocks(
        localCurrency: String
    ): Single<CollateralLocks> =
        api.getWithdrawalLocks(localCurrency)
            .map { it.toWithdrawalLocks() }
}

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

fun String.toMobilePaymentType(): MobilePaymentType =
    when (this) {
        CardResponse.GOOGLE_PAY -> MobilePaymentType.GOOGLE_PAY
        CardResponse.APPLE_PAY -> MobilePaymentType.APPLE_PAY
        else -> MobilePaymentType.UNKNOWN
    }
