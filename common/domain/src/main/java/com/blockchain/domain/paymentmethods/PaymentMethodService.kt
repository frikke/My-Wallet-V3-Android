package com.blockchain.domain.paymentmethods

import com.blockchain.domain.paymentmethods.model.EligiblePaymentMethodType
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
import com.blockchain.domain.paymentmethods.model.PaymentMethodTypeWithEligibility
import com.blockchain.outcome.Outcome
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

interface PaymentMethodService {
    suspend fun getPaymentMethodDetailsForId(
        paymentId: String
    ): Outcome<Exception, PaymentMethodDetails>

    fun getAvailablePaymentMethodsTypes(
        fiatCurrency: FiatCurrency,
        fetchSddLimits: Boolean,
        onlyEligible: Boolean
    ): Single<List<PaymentMethodTypeWithEligibility>>

    fun getEligiblePaymentMethodTypes(
        fiatCurrency: FiatCurrency
    ): Single<List<EligiblePaymentMethodType>>

    fun getLinkedPaymentMethods(
        currency: FiatCurrency
    ): Single<List<LinkedPaymentMethod>>
}
