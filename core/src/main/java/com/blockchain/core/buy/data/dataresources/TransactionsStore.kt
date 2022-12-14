package com.blockchain.core.buy.data.dataresources

import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class TransactionsStore(
    private val nabuService: NabuService
) : KeyedStore<TransactionsStore.Key, TransactionsResponse> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = "TransactionsStore",
        fetcher = Fetcher.Keyed.ofSingle { key ->
            nabuService.getTransactions(
                product = key.product.toRequestString(),
                type = key.type
            )
        },
        keySerializer = Key.serializer(),
        dataSerializer = TransactionsResponse.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_MINUTE)
    ),
    KeyedFlushableDataSource<TransactionsStore.Key> {

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    @Serializable
    data class Key(
        val product: Product,
        val type: String?
    )
}

private fun Product.toRequestString(): String =
    when (this) {
        Product.TRADE -> "SWAP"
        Product.BUY,
        Product.SELL -> "SIMPLEBUY"
        else -> this.toString()
    }