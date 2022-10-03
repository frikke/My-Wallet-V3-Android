package com.blockchain.domain.fiatcurrencies

import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.outcome.Outcome
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.Flow

interface FiatCurrenciesService {

    val selectedTradingCurrency: FiatCurrency

    // todo(othman) rename & refactor
    @Deprecated("use getTradingCurrenciesFlow")
    suspend fun getTradingCurrencies(fresh: Boolean = false): Outcome<Exception, TradingCurrencies>

    fun getTradingCurrenciesFlow(): Flow<TradingCurrencies>

    suspend fun setSelectedTradingCurrency(currency: FiatCurrency): Outcome<Exception, Unit>
}
