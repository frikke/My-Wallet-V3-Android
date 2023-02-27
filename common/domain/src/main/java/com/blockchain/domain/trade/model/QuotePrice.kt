package com.blockchain.domain.trade.model

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money

data class QuotePrice(
    val currencyPair: CurrencyPair,
    val amount: Money, // source curr
    val price: Money, // price, in target curr, for each "major unit" of source curr, takes into account fees
    val rawPrice: Money, // price, in target curr, for each "major unit" of source curr, does not take into account fees
    val resultAmount: Money, // target curr : (amount(major)-dynamicFee)*price - networkFee
    val dynamicFee: Money, // source curr
    val networkFee: Money?, // destination curr
    val paymentMethod: PaymentMethodType,
    val orderProfileName: String
)
