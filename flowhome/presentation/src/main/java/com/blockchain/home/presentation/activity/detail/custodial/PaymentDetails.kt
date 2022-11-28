package com.blockchain.home.presentation.activity.detail.custodial

import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.PaymentMethodType

data class PaymentDetails(
    val paymentMethodId: String,
    val label: String? = null,
    val endDigits: String? = null,
    val accountType: String? = null,
    val paymentMethodType: PaymentMethodType? = null,
    val mobilePaymentType: MobilePaymentType? = null
)
