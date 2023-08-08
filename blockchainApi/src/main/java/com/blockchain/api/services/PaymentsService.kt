package com.blockchain.api.services

import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.payments.data.CardTokenIdResponse
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.WithdrawalLocksResponse
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializable

class PaymentsService internal constructor(
    private val api: PaymentsApi
) {
    suspend fun getPaymentMethodDetailsForId(
        paymentId: String
    ): Outcome<Exception, PaymentMethodDetailsResponse> =
        api.getPaymentMethodDetailsForId(paymentId)

    suspend fun getCardTokenId(): Outcome<Exception, CardTokenIdResponse> = api.getCardTokenId()

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
                date = lockPeriod.expiresAt,
                buyCurrency = lockPeriod.bought?.currency,
                buyValue = lockPeriod.bought?.amount
            )
        }
    )

@Serializable
data class CollateralLocks(
    val currency: String,
    val value: String,
    val locks: List<CollateralLock>
)

@Serializable
data class CollateralLock(
    val currency: String,
    val value: String,
    val date: String,
    // Used for locks on purchases
    val buyCurrency: String?,
    val buyValue: String?
)

fun String.toMobilePaymentType(): MobilePaymentType =
    when (this) {
        CardResponse.GOOGLE_PAY -> MobilePaymentType.GOOGLE_PAY
        CardResponse.APPLE_PAY -> MobilePaymentType.APPLE_PAY
        else -> MobilePaymentType.UNKNOWN
    }
