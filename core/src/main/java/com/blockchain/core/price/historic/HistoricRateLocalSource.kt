package com.blockchain.core.price.historic

import com.blockchain.core.Database
import com.blockchain.core.price.ExchangeRate
import com.squareup.sqldelight.runtime.rx3.asObservable
import com.squareup.sqldelight.runtime.rx3.mapToOne
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

internal class HistoricRateLocalSource(private val database: Database) {
    fun get(
        selectedFiat: String,
        asset: AssetInfo,
        requestedTimestamp: Long
    ): Single<ExchangeRate> =
        database.historicRateQueries
            .selectByKeys(asset.networkTicker, selectedFiat, requestedTimestamp)
            .asObservable()
            .mapToOne()
            .map {
                ExchangeRate.CryptoToFiat(
                    from = asset,
                    to = selectedFiat,
                    rate = it.price.toBigDecimal()
                ) as ExchangeRate
            }.firstOrError()

    fun insert(selectedFiat: String, asset: AssetInfo, requestedTimestamp: Long, price: Double) {
        database.historicRateQueries.insert(asset.networkTicker, selectedFiat, price, requestedTimestamp)
    }
}
