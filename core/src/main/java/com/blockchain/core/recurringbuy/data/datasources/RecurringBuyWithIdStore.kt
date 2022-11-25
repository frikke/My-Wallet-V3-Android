package com.blockchain.core.recurringbuy.data.datasources

import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.common.extensions.wrapErrorMessage
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyResponse
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

internal class RecurringBuyWithIdStore(
    private val nabu: Nabu
) : KeyedStore<RecurringBuyWithIdStore.Key, List<RecurringBuyResponse>> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle { key ->
            nabu.getRecurringBuyById(
                recurringBuyId = key.recurringBuyId
            ).wrapErrorMessage()
        },
        keySerializer = Key.serializer(),
        dataSerializer = ListSerializer(RecurringBuyResponse.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<RecurringBuyWithIdStore.Key> {

    @Serializable
    data class Key(
        val recurringBuyId: String,
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
