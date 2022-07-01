package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.ethereum.evm.BalancesResponse
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.StoreKey
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import java.util.concurrent.atomic.AtomicReference

class Erc20L2Store(
    private val evmService: NonCustodialEvmService,
) : KeyedStore<Erc20L2Store.Key, Throwable, BalancesResponse> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                rxSingleOutcome {
                    evmService.getBalances(key.accountHash, key.networkTicker)
                }
            },
            errorMapper = {
                it
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = BalancesResponse.serializer(),
        mediator = FreshnessMediator(Freshness.ofMinutes(60L)) // todo(othman) duration?
    ),
    Erc20L2DataSource {

    @Serializable
    data class Key(
        val accountHash: String,
        val networkTicker: String
    ) : StoreKey

    private val accountAtomic = AtomicReference<String>()

    override fun stream(
        accountHash: String,
        networkTicker: String,
        refresh: Boolean
    ): Flow<StoreResponse<Throwable, BalancesResponse>> {
        // get old account and save new accountHash
        val oldAccountHash = accountAtomic.getAndSet(accountHash)

        // if old/new accounts are different, invalidate old store
        if (oldAccountHash != null && oldAccountHash != accountHash) {
            invalidate(accountHash = oldAccountHash, networkTicker = networkTicker)
        }

        return stream(
            KeyedStoreRequest.Cached(
                key = Key(accountHash = accountHash, networkTicker = networkTicker),
                forceRefresh = refresh
            )
        )
    }

    override fun invalidate(param: String) {
        val accountHash = accountAtomic.get()
        if (accountHash.isNotEmpty()) {
            invalidate(accountHash = accountHash, networkTicker = param)
        }
    }

    private fun invalidate(accountHash: String, networkTicker: String) {
        markAsStale(key = Key(accountHash = accountHash, networkTicker = networkTicker))
    }

    companion object {
        private const val STORE_ID = "Erc20L2Store"
    }
}