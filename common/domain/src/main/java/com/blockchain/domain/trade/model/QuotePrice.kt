package com.blockchain.domain.trade.model

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money

data class QuotePrice(
    val currencyPair: CurrencyPair,
    val amount: Money,
    val price: Money,
    val resultAmount: Money,
    val dynamicFee: Money,
    val networkFee: Money?,
    val paymentMethod: PaymentMethodType,
    val orderProfileName: String
)
