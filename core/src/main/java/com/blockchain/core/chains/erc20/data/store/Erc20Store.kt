package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.core.chains.erc20.data.domain.Erc20TokenBalanceStore
import com.blockchain.core.chains.erc20.data.domain.toDomain
import com.blockchain.core.chains.erc20.data.domain.toStore
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.Mediator
import com.blockchain.store.StoreResponse
import com.blockchain.store.mapListData
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.internal.cacheGet
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal class Erc20Store(
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
        mediator = object : Mediator<Key, List<Erc20TokenBalanceStore>> {
            fun shouldFetch(requestAccountHash: String, cachedAccountHash: String, dataAgeMillis: Long): Boolean {
                return when {
                    requestAccountHash != cachedAccountHash -> true
                    else -> dataAgeMillis > TimeUnit.HOURS.toMillis(1L)
                }
            }

            override fun shouldFetch(
                requestKey: Key,
                cachedData: CachedData<Key, List<Erc20TokenBalanceStore>>?
            ): Boolean {
                cachedData ?: return true

                return shouldFetch(
                    requestAccountHash = requestKey.accountHash,
                    cachedAccountHash = cachedData.key.accountHash,
                    dataAgeMillis = Calendar.getInstance().timeInMillis - cachedData.lastFetched
                )
            }
        }
    ),
    Erc20DataSource {

    @Serializable
    data class Key(
        val accountHash: String
    )

    override fun stream(
        accountHash: String,
        refresh: Boolean
    ): Flow<StoreResponse<Throwable, List<Erc20TokenBalance>>> {
        return stream(
            KeyedStoreRequest.Cached(
                key = Key(accountHash),
                forceRefresh = refresh
            )
        ).mapListData { it.toDomain() }
    }

    override fun invalidate() {
//        val accountHash = accountAtomic.get()
//        if (accountHash.isNotEmpty()) {
//            invalidate(accountHash = accountHash)
//        }
    }

    private fun invalidate(accountHash: String) {
        println("----- ::invalidate Erc200000")
        markAsStale(key = Key(accountHash = accountHash))
    }

    companion object {
        private const val STORE_ID = "Erc20Store"
    }
}
