package com.blockchain.domain.fiatcurrencies

import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.outcome.Outcome
import info.blockchain.balance.FiatCurrency

interface FiatCurrenciesService {

    val selectedTradingCurrency: FiatCurrency

    suspend fun getTradingCurrencies(fresh: Boolean = false): Outcome<Exception, TradingCurrencies>

    suspend fun setSelectedTradingCurrency(currency: FiatCurrency): Outcome<Exception, Unit>
}
