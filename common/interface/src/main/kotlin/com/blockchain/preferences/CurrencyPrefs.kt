package com.blockchain.preferences

interface CurrencyPrefs {
    var selectedFiatCurrency: String
    var tradingCurrency: String
    val defaultFiatCurrency: String
}
