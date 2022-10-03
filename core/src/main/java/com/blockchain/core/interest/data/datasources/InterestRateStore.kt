package com.blockchain.core.interest.data.datasources

import com.blockchain.api.interest.InterestApiService
import com.blockchain.api.interest.data.InterestRateDto
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class InterestRateStore(
    private val authenticator: Authenticator,
    private val interestApiService: InterestApiService,
) : KeyedStore<InterestRateStore.Key, InterestRateDto> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                authenticator.authenticate { token ->
                    interestApiService.getInterestRates(token.authHeader, key.cryptoCurrencyTicker)
                        .defaultIfEmpty(InterestRateDto(rate = 0.0))
                }
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = InterestRateDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<InterestRateStore.Key> {

    @Serializable
    data class Key(
        val cryptoCurrencyTicker: String,
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestRateStore"
    }
}
