package com.blockchain.api.fiatcurrencies.data

import kotlinx.serialization.Serializable

@Serializable
data class SetSelectedTradingCurrencyRequest(
    val fiatTradingCurrency: String
)
