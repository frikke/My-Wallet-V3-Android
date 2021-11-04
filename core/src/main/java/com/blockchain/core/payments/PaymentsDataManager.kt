package com.blockchain.core.payments

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.adapters.Outcome
import com.blockchain.api.adapters.mapLeft
import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.api.services.PaymentsService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.payments.model.FundsLock
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.core.payments.model.PaymentMethodDetailsError
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

interface PaymentsDataManager {
    suspend fun getPaymentMethodDetailsForId(
        paymentId: String
    ): Outcome<PaymentMethodDetailsError, PaymentMethodDetails>

    fun getWithdrawalLocks(localCurrency: String): Single<FundsLocks>
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

    override fun getWithdrawalLocks(localCurrency: String): Single<FundsLocks> =
        authenticator.getAuthHeader().flatMap {
            paymentsService.getWithdrawalLocks(it, localCurrency)
                .map { locks ->
                    FundsLocks(
                        onHoldTotalAmount = FiatValue.fromMinor(locks.currency, locks.value.toLong()),
                        locks = locks.locks.map { lock ->
                            FundsLock(
                                amount = FiatValue.fromMinor(lock.currency, lock.value.toLong()),
                                date = lock.date.toZonedDateTime()
                            )
                        }
                    )
                }
        }
}