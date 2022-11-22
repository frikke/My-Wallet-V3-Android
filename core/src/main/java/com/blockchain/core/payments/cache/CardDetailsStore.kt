package com.blockchain.core.payments.cache

import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class CardDetailsStore(
    private val paymentMethodsService: PaymentMethodsService
) : KeyedStore<CardDetailsStore.Key,
    CardResponse
    > by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = "CardDetailsStore",
    fetcher = Fetcher.Keyed.ofSingle(
        mapper = { key ->
            paymentMethodsService.getCardDetails(key.cardId)
        },
    ),
    dataSerializer = CardResponse.serializer(),
    keySerializer = Key.serializer(),
    mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
), KeyedFlushableDataSource<CardDetailsStore.Key> {

    @Serializable
    data class Key(
        val cardId: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }
}
