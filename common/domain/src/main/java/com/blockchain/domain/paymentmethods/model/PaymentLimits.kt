package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.io.Serializable
import java.math.BigInteger

data class PaymentLimits internal constructor(override val min: Money, override val max: Money) :
    Serializable,
    LegacyLimits {
    constructor(min: BigInteger, max: BigInteger, currency: Currency) : this(
        Money.fromMinor(currency, min),
        Money.fromMinor(currency, max)
    )
}
