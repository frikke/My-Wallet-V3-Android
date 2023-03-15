package com.blockchain.domain.paymentmethods.model

enum class PaymentMethodTypeCapability {
    DEPOSIT,
    WITHDRAWAL,
    BROKERAGE, // can be used in a brokerage order, for example when Buying
}
