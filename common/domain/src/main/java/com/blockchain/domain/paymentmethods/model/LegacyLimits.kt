package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.Money

interface LegacyLimits {
    val min: Money
    val max: Money?
    val currency: String
        get() = min.currencyCode
}
