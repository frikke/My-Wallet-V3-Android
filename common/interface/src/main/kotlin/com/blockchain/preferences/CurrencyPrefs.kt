package com.blockchain.preferences

import info.blockchain.balance.FiatCurrency

interface CurrencyPrefs {
    var selectedFiatCurrency: FiatCurrency
    var tradingCurrency: FiatCurrency?
    val noCurrencySet: Boolean
}
