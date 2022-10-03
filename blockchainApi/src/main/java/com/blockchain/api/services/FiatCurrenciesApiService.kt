package com.blockchain.api.services

import com.blockchain.api.fiatcurrencies.FiatCurrenciesApi
import com.blockchain.api.fiatcurrencies.data.SetSelectedTradingCurrencyRequest

class FiatCurrenciesApiService(
    private val api: FiatCurrenciesApi
) {

    suspend fun setSelectedTradingCurrency(
        authHeader: String,
        currency: String
    ) = api.setSelectedTradingCurrency(
        authHeader,
        SetSelectedTradingCurrencyRequest(
            fiatTradingCurrency = currency
        )
    )
}
