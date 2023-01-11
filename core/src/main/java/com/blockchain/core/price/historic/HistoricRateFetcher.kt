package com.blockchain.core.price.historic

import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.store.mapData
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow

// Moved here fromm app. This should be under, rather than over, ExchangeRateDM TODO
class HistoricRateFetcher internal constructor(
    private val historicRateStore: HistoricRateStore,
) {
    fun fetch(
        asset: Currency,
        selectedFiat: FiatCurrency,
        timestampMs: Long,
        value: Money
    ): Flow<DataResource<Money>> =
        historicRateStore.stream(
            KeyedFreshnessStrategy.Cached(
                HistoricRateStore.Key(
                    fiatTicker = selectedFiat.networkTicker,
                    assetTicker = asset.networkTicker,
                    requestedTimestamp = timestampMs,
                ),
                RefreshStrategy.RefreshIfStale
            )
        ).mapData {
            ExchangeRate(
                rate = it.rate.toBigDecimal(),
                from = asset,
                to = selectedFiat,
            ).convert(value)
        }
}
