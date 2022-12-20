package com.blockchain.core.price.historic

import com.blockchain.api.services.AssetPriceService
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.Serializable

class HistoricRateStore(
    private val assetPriceService: AssetPriceService,
) : KeyedStore<HistoricRateStore.Key, HistoricRate> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle { key ->
            assetPriceService.getHistoricPrices(
                baseTickers = setOf(key.assetTicker),
                quoteTickers = setOf(key.fiatTicker),
                time = key.requestedTimestamp / 1000, // API uses seconds
            ).map {
                HistoricRate(
                    rate = it.first().price,
                    fiatTicker = key.fiatTicker,
                    assetTicker = key.assetTicker,
                    requestedTimestamp = key.requestedTimestamp,
                )
            }
        },
        keySerializer = Key.serializer(),
        dataSerializer = HistoricRate.serializer(),
        mediator = object : Mediator<Key, HistoricRate> {
            override fun shouldFetch(cachedData: CachedData<Key, HistoricRate>?): Boolean =
                cachedData == null
        }
    ) {

    @Serializable
    data class Key(
        val fiatTicker: String,
        val assetTicker: String,
        val requestedTimestamp: Long,
    )

    companion object {
        private const val STORE_ID = "HistoricRateStore"
    }
}
