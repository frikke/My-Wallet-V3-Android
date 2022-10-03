package com.blockchain.core.fiatcurrencies

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import info.blockchain.balance.FiatCurrency
import java.io.Serializable

sealed class CurrencySelectionAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = mapOf()
) : AnalyticsEvent {
    data class TradingCurrencyChanged(
        val currency: FiatCurrency
    ) : CurrencySelectionAnalytics(
        AnalyticsNames.CURRENCY_SELECTION_TRADING_CURRENCY_CHANGED.eventName,
        mapOf("currency" to currency.networkTicker)
    )
}
