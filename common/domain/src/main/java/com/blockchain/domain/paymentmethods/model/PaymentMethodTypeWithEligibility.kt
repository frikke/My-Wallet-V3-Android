package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.FiatCurrency

data class PaymentMethodTypeWithEligibility(
    val eligible: Boolean,
    val currency: FiatCurrency,
    val type: PaymentMethodType,
    val limits: PaymentLimits,
    val cardFundSources: List<String>?,
    // optional since only ACH will support it initially, if null then we assume all capabilities are present
    val capabilities: List<PaymentMethodTypeCapability>?,
)
