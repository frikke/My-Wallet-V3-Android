package com.blockchain.core.price.historic

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

internal class HistoricRateRemoteSource(private val exchangeRates: ExchangeRatesDataManager) {
    fun get(asset: AssetInfo, timeStampMs: Long): Single<ExchangeRate> {
        return exchangeRates.getHistoricRate(
            fromAsset = asset,
            secSinceEpoch = timeStampMs / 1000 // API uses seconds
        )
    }
}