package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.ethereum.evm.BalancesResponse
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.erc20.data.domain.Erc20L2BalancesStore
import com.blockchain.core.chains.erc20.data.domain.toDomain
import com.blockchain.core.chains.erc20.data.domain.toStore
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.outcome.map
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store.mapData
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.utils.rxSingleOutcome
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class Erc20L2Store(
    private val evmService: NonCustodialEvmService,
    private val ethDataManager: EthDataManager,
) : KeyedStore<Erc20L2Store.Key, Erc20L2BalancesStore> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { key ->
                rxSingleOutcome {
                    evmService.getBalances(ethDataManager.accountAddress, key.networkTicker)
                        .map { it.toStore(ethDataManager.accountAddress) }
                }
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = Erc20L2BalancesStore.serializer(),
        mediator = object : Mediator<Key, Erc20L2BalancesStore> {
            /**
             * when accountHash changes -> fetch fresh
             * otherwise 1 hour
             */
            private fun shouldFetch(
                requestAccountHash: String,
                cachedAccountHash: String,
                dataAgeMillis: Long
            ): Boolean {
                return when {
                    requestAccountHash != cachedAccountHash -> true
                    else -> dataAgeMillis > TimeUnit.HOURS.toMillis(1L)
                }
            }

            override fun shouldFetch(cachedData: CachedData<Key, Erc20L2BalancesStore>?): Boolean {
                cachedData ?: return true

                return shouldFetch(
                    requestAccountHash = ethDataManager.accountAddress,
                    cachedAccountHash = cachedData.data.accountHash,
                    dataAgeMillis = Calendar.getInstance().timeInMillis - cachedData.lastFetched
                )
            }
        }
    ),
    Erc20L2DataSource {

    @Serializable
    data class Key(
        val networkTicker: String
    )

    override fun streamData(
        request: KeyedFreshnessStrategy<Key>
    ): Flow<DataResource<BalancesResponse>> {
        return stream(request).mapData { it.toDomain() }
    }

    override fun invalidate(param: String) {
        markAsStale(key = Key(networkTicker = param))
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "Erc20L2Store"
    }
}
