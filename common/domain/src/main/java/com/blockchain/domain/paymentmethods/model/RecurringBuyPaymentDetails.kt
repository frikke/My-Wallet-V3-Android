package com.blockchain.domain.paymentmethods.model

import java.io.Serializable

interface RecurringBuyPaymentDetails : Serializable {
    val paymentDetails: PaymentMethodType
}
