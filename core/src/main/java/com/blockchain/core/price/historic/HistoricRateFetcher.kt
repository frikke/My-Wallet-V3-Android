package com.blockchain.core.price.historic

import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.store.firstOutcome
import com.blockchain.store.mapData
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

// Moved here fromm app. This should be under, rather than over, ExchangeRateDM TODO
class HistoricRateFetcher internal constructor(
    private val historicRateStore: HistoricRateStore,
) {
    fun fetch(asset: AssetInfo, selectedFiat: FiatCurrency, timestampMs: Long, value: Money): Single<Money> =
        rxSingleOutcome {
            historicRateStore.stream(
                KeyedFreshnessStrategy.Cached(
                    HistoricRateStore.Key(
                        fiatTicker = selectedFiat.networkTicker,
                        assetTicker = asset.networkTicker,
                        requestedTimestamp = timestampMs,
                    ),
                    false
                )
            ).firstOutcome()
                .map {
                    ExchangeRate(
                        rate = it.rate.toBigDecimal(),
                        from = asset,
                        to = selectedFiat,
                    ).convert(value)
                }
        }

    fun fetch2(
        asset: AssetInfo, selectedFiat: FiatCurrency, timestampMs: Long,
        value: Money
    ): Flow<DataResource<Money>> =
        historicRateStore.stream(
            KeyedFreshnessStrategy.Cached(
                HistoricRateStore.Key(
                    fiatTicker = selectedFiat.networkTicker,
                    assetTicker = asset.networkTicker,
                    requestedTimestamp = timestampMs,
                ),
                false
            )
        ).mapData {
            ExchangeRate(
                rate = it.rate.toBigDecimal(),
                from = asset,
                to = selectedFiat,
            ).convert(value)
        }
}
