package com.blockchain.core.payments.cache

import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.services.PaymentsService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

class PaymentMethodsStore(
    private val paymentsService: PaymentsService
) : KeyedStore<
    PaymentMethodsStore.Key,
    PaymentMethodDetailsResponse
    > by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = "PaymentMethodsStore",
    fetcher = Fetcher.Keyed.ofOutcome { key ->
        paymentsService.getPaymentMethodDetailsForId(key.paymentId)
    },
    dataSerializer = PaymentMethodDetailsResponse.serializer(),
    keySerializer = Key.serializer(),
    mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
),
    KeyedFlushableDataSource<PaymentMethodsStore.Key> {

    @Serializable
    data class Key(
        val paymentId: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }
}
