package com.blockchain.core.payments

import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.api.services.PaymentsService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.payments.model.FundsLock
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single

interface PaymentsDataManager {
    fun getPaymentMethodDetailsForId(paymentId: String): Single<PaymentMethodDetails>
    fun getWithdrawalLocks(localCurrency: String): Single<FundsLocks>
}

class PaymentsDataManagerImpl(
    private val paymentsService: PaymentsService,
    private val authenticator: AuthHeaderProvider
) : PaymentsDataManager {

    override fun getPaymentMethodDetailsForId(paymentId: String): Single<PaymentMethodDetails> =
        authenticator.getAuthHeader().flatMap {
            paymentsService.getPaymentMethodDetailsForId(
                it,
                paymentId
            )
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