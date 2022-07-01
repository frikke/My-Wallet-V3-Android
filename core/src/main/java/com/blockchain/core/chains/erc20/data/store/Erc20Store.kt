package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.core.chains.erc20.data.domain.Erc20TokenBalanceStore
import com.blockchain.core.chains.erc20.data.domain.toDomain
import com.blockchain.core.chains.erc20.data.domain.toStore
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store.mapListData
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.StoreKey
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicReference

class Erc20Store(
    private val erc20Service: NonCustodialErc20Service
) : KeyedStore<Erc20Store.Key, Throwable, List<Erc20TokenBalanceStore>> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                erc20Service.getTokenBalances(key.accountHash)
                    .mapList { it.toStore() }
            },
            errorMapper = {
                it
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = ListSerializer(Erc20TokenBalanceStore.serializer()),
        mediator = FreshnessMediator(Freshness.ofMinutes(60L)) // todo(othman) duration?
    ),
    Erc20DataSource {

    @Serializable
    data class Key(
        val accountHash: String
    ) : StoreKey

    private val accountAtomic = AtomicReference<String>()

    override fun stream(
        accountHash: String,
        refresh: Boolean
    ): Flow<StoreResponse<Throwable, List<Erc20TokenBalance>>> {
        // get old account and save new accountHash
        val oldAccountHash = accountAtomic.getAndSet(accountHash)

        // if old/new accounts are different, invalidate old store
        if (oldAccountHash != null && oldAccountHash != accountHash) {
            invalidate(accountHash = oldAccountHash)
        }

        return stream(
            KeyedStoreRequest.Cached(
                key = Key(accountHash),
                forceRefresh = refresh
            )
        ).mapListData { it.toDomain() }
    }

    override fun invalidate() {
        val accountHash = accountAtomic.get()
        if (accountHash.isNotEmpty()) {
            invalidate(accountHash = accountHash)
        }
    }

    private fun invalidate(accountHash: String) {
        markAsStale(key = Key(accountHash = accountHash))
    }

    companion object {
        private const val STORE_ID = "Erc20Store"
    }
}
