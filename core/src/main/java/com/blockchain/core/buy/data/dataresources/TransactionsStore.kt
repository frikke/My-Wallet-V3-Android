package com.blockchain.core.buy.data.dataresources

import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder

class TransactionsStore(
    private val nabuService: NabuService
) : KeyedStore<TransactionsStore.Key, TransactionsResponse> by InMemoryCacheStoreBuilder()
    .buildKeyed(
        storeId = "TransactionsStore",
        fetcher = Fetcher.Keyed.ofSingle { key ->
            nabuService.getTransactions(
                product = key.product.toRequestString(),
                type = key.type
            )
        },
        mediator = FreshnessMediator(Freshness.DURATION_1_MINUTE)
    ) {
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
