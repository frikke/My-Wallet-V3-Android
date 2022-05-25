package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.FiatCurrency

data class EligiblePaymentMethodType(
    val type: PaymentMethodType,
    val currency: FiatCurrency
)
