package com.blockchain.core.paymentMethods

import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.api.services.PaymentMethodService
import com.blockchain.auth.AuthHeaderProvider
import io.reactivex.rxjava3.core.Single

interface PaymentMethodsDataManager {
    fun getPaymentMethodDetailsForId(paymentId: String): Single<PaymentMethodDetails>
}

class PaymentMethodsDataManagerImpl(
    private val paymentMethodService: PaymentMethodService,
    private val authenticator: AuthHeaderProvider
) : PaymentMethodsDataManager {

    override fun getPaymentMethodDetailsForId(paymentId: String): Single<PaymentMethodDetails> =
        authenticator.getAuthHeader().flatMap {
            paymentMethodService.getPaymentMethodDetailsForId(
                it,
                paymentId
            )
        }.map { it }
}