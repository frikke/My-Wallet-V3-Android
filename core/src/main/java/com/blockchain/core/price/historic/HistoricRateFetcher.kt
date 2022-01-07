package com.blockchain.core.price.historic

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

// Moved here fromm app. This should be under, rather than over, ExchangeRateDM TODO
class HistoricRateFetcher internal constructor(
    private val localSource: HistoricRateLocalSource,
    private val remoteSource: HistoricRateRemoteSource
) {
    fun fetch(asset: AssetInfo, selectedFiat: String, timestampMs: Long, value: Money): Single<Money> {
        return localSource.get(selectedFiat, asset, timestampMs).onErrorResumeNext {
            remoteSource.get(asset, timestampMs).doOnSuccess {
                localSource.insert(selectedFiat, asset, timestampMs, it.price().toBigDecimal().toDouble())
            }
        }.map {
            it.convert(value)
        }
    }
}
