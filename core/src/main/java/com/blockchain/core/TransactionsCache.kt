package com.blockchain.core

import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.Serializable

@Serializable
data class TransactionsRequest(
    val product: String,
    val type: String?
)

class TransactionsStore(private val nabuService: NabuService) :
    KeyedStore<TransactionsRequest, TransactionsResponse> by
    PersistedJsonSqlDelightStoreBuilder().buildKeyed(
        storeId = "TransactionsStore",
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                nabuService.getTransactions(
                    product = key.product,
                    type = key.type
                )
            }
        ),
        keySerializer = TransactionsRequest.serializer(),
        dataSerializer = TransactionsResponse.serializer(),
        mediator = object : Mediator<TransactionsRequest, TransactionsResponse> {
            override fun shouldFetch(cachedData: CachedData<TransactionsRequest, TransactionsResponse>?): Boolean {
                return cachedData == null || cachedData.lastFetched == 0L
            }
        }
    ),
    FlushableDataSource {
    override fun invalidate() {
        markStoreAsStale()
    }
}
