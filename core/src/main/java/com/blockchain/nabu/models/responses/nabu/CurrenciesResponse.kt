package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
data class CurrenciesResponse(
    // Selected Trading Currency
    val preferredFiatTradingCurrency: String,
    // Supported Trading Currencies
    val usableFiatCurrencies: List<String>,
    // Selected Display Currency
    val defaultWalletCurrency: String,
    // Trading Currencies to be displayed in the Portfolio and Sell
    val userFiatCurrencies: List<String>
)
