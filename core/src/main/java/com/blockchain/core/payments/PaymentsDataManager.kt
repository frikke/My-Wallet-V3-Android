package com.blockchain.core.payments

import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.api.services.PaymentsService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.payments.model.WithdrawalLock
import com.blockchain.core.payments.model.WithdrawalsLocks
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single

interface PaymentsDataManager {
    fun getPaymentMethodDetailsForId(paymentId: String): Single<PaymentMethodDetails>

    fun getWithdrawalLocks(localCurrency: String): Single<WithdrawalsLocks>
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

    override fun getWithdrawalLocks(localCurrency: String): Single<WithdrawalsLocks> =
        authenticator.getAuthHeader().flatMap {
            paymentsService.getWithdrawalLocks(it, localCurrency).map {
                WithdrawalsLocks(
                    onHoldTotalAmount = it.totalAmount.let {
                        FiatValue.fromMinor(it.currency, it.value.toLong())
                    },
                    locks = it.locks.map { lock ->
                        WithdrawalLock(
                            amount = FiatValue.fromMinor(lock.amount.currency, lock.amount.value.toLong()),
                            date = lock.date.toZonedDateTime()
                        )
                    }
                )
            }
        }
}