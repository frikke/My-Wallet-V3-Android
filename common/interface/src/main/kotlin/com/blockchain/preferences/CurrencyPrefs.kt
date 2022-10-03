package com.blockchain.preferences

import info.blockchain.balance.FiatCurrency

interface CurrencyPrefs {
    var selectedFiatCurrency: FiatCurrency
    // TODO(aromano): I'll remove this once I've fully migrated this CurrencyPrefs to the FiatCurrencyService
    @Deprecated("Use FiatCurrenciesService.setSelectedTradingCurrency instead", level = DeprecationLevel.ERROR)
    var tradingCurrency: FiatCurrency?
    val noCurrencySet: Boolean
}
