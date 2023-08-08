package com.blockchain.core.recurringbuy.domain.model

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails

data class FundsAccount(val currency: String) : RecurringBuyPaymentDetails {
    override val paymentDetails: PaymentMethodType
        get() = PaymentMethodType.FUNDS
}
