package com.blockchain.core.payments

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.api.services.PaymentsService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.payments.model.FundsLock
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.core.payments.model.PaymentMethodDetailsError
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.mapLeft
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

interface PaymentsDataManager {
    suspend fun getPaymentMethodDetailsForId(
        paymentId: String
    ): Outcome<PaymentMethodDetailsError, PaymentMethodDetails>

    fun getWithdrawalLocks(localCurrency: Currency): Single<FundsLocks>
}
class PaymentsDataManagerImpl(
    private val paymentsService: PaymentsService,
    private val authenticator: AuthHeaderProvider
) : PaymentsDataManager {

    override suspend fun getPaymentMethodDetailsForId(
        paymentId: String
    ): Outcome<PaymentMethodDetailsError, PaymentMethodDetails> {
        // TODO Turn getAuthHeader() into a suspension function
        val auth = authenticator.getAuthHeader().await()
        return paymentsService.getPaymentMethodDetailsForId(auth, paymentId).mapLeft { apiError: ApiError ->
            when (apiError) {
                is ApiError.HttpError -> PaymentMethodDetailsError.REQUEST_FAILED
                is ApiError.NetworkError -> PaymentMethodDetailsError.SERVICE_UNAVAILABLE
                is ApiError.UnknownApiError -> PaymentMethodDetailsError.UNKNOWN
            }
        }
    }

    override fun getWithdrawalLocks(localCurrency: Currency): Single<FundsLocks> =
        authenticator.getAuthHeader().flatMap {
            paymentsService.getWithdrawalLocks(it, localCurrency.networkTicker)
                .map { locks ->
                    FundsLocks(
                        onHoldTotalAmount = Money.fromMinor(localCurrency, locks.value.toBigInteger()),
                        locks = locks.locks.map { lock ->
                            FundsLock(
                                amount = Money.fromMinor(localCurrency, lock.value.toBigInteger()),
                                date = lock.date.toZonedDateTime()
                            )
                        }
                    )
                }
        }
}
