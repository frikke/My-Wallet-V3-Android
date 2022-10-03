package com.blockchain.domain.paymentmethods.model

data class PaymentMethodDetails(
    val label: String? = null,
    val endDigits: String? = null,
    val mobilePaymentType: MobilePaymentType? = null
)
