package com.blockchain.core.price.historic

import com.blockchain.core.Database
import com.blockchain.core.price.ExchangeRate
import com.squareup.sqldelight.runtime.rx3.asObservable
import com.squareup.sqldelight.runtime.rx3.mapToOne
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

internal class HistoricRateLocalSource(private val database: Database) {
    fun get(
        selectedFiat: FiatCurrency,
        asset: AssetInfo,
        requestedTimestamp: Long
    ): Single<ExchangeRate> =
        database.historicRateQueries
            .selectByKeys(asset.networkTicker, selectedFiat.networkTicker, requestedTimestamp)
            .asObservable()
            .mapToOne()
            .map {
                ExchangeRate(
                    from = asset,
                    to = selectedFiat,
                    rate = it.price.toBigDecimal()
                ) as ExchangeRate
            }.firstOrError()

    fun insert(selectedFiat: FiatCurrency, asset: AssetInfo, requestedTimestamp: Long, price: Double) {
        database.historicRateQueries.insert(asset.networkTicker, selectedFiat.networkTicker, price, requestedTimestamp)
    }
}
