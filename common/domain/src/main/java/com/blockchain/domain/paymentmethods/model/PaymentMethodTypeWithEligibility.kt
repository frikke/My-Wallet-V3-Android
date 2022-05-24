package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.FiatCurrency

data class PaymentMethodTypeWithEligibility(
    val eligible: Boolean,
    val currency: FiatCurrency,
    val type: PaymentMethodType,
    val limits: PaymentLimits,
    val cardFundSources: List<String>? = null
)
