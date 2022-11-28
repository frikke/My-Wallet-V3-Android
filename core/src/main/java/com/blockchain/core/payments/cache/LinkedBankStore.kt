package com.blockchain.core.payments.cache

import com.blockchain.api.payments.data.LinkedBankTransferResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class LinkedBankStore(
    private val paymentMethodsService: PaymentMethodsService
) : KeyedStore<LinkedBankStore.Key,
    LinkedBankTransferResponse
    > by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = "LinkedBankStore",
    fetcher = Fetcher.Keyed.ofSingle { key ->
        paymentMethodsService.getLinkedBank(id = key.id)
    },
    dataSerializer = LinkedBankTransferResponse.serializer(),
    keySerializer = Key.serializer(),
    mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
),
    KeyedFlushableDataSource<LinkedBankStore.Key> {

    @Serializable
    data class Key(
        val id: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }
}
