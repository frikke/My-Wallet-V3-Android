package com.blockchain.core.history.data.datasources

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.serialization.Serializable

// todo(othman) refactor dto
// todo(othman) rename?
class PaymentTransactionHistoryStore(
    private val authenticator: Authenticator,
    private val nabuService: NabuService,
) : KeyedStore<PaymentTransactionHistoryStore.Key, TransactionsResponse> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                authenticator.authenticate { token ->
                    nabuService.getTransactions(
                        sessionToken = token,
                        product = key.product,
                        type = key.type
                    )
                }
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = TransactionsResponse.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<PaymentTransactionHistoryStore.Key> {

    @Serializable
    data class Key(
        val product: String,
        val type: String?,
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
