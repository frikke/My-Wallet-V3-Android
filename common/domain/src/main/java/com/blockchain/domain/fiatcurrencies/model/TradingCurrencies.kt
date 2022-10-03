package com.blockchain.domain.fiatcurrencies.model

import info.blockchain.balance.FiatCurrency

data class TradingCurrencies(
    // Default trading currency, used in Buy
    val selected: FiatCurrency,
    // Limited set of currencies, based on region, that the user should interact with,
    // used to display Funds in Portfolio and Sell, eg. ARG user will have: [ARS, USD]
    val allRecommended: List<FiatCurrency>,
    // All the trading currencies available for this user, eg. ARG user will have [ARS, USD, EUR, GBP]
    val allAvailable: List<FiatCurrency>,
)
