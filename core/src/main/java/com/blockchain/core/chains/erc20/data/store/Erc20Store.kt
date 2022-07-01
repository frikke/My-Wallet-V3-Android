package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.core.chains.erc20.data.domain.Erc20TokenBalancesStore
import com.blockchain.core.chains.erc20.data.domain.toDomain
import com.blockchain.core.chains.erc20.data.domain.toStore
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store.mapData
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

internal class Erc20Store(
    private val erc20Service: NonCustodialErc20Service,
    private val ethDataManager: EthDataManager,
) : Store<Throwable, Erc20TokenBalancesStore> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                erc20Service.getTokenBalances(ethDataManager.accountAddress)
                    .map {
                        it.toStore(accountHash = ethDataManager.accountAddress)
                    }
            },
            errorMapper = {
                it
            }
        ),
        dataSerializer = Erc20TokenBalancesStore.serializer(),
        mediator = object : Mediator<Unit, Erc20TokenBalancesStore> {
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

            override fun shouldFetch(cachedData: CachedData<Unit, Erc20TokenBalancesStore>?): Boolean {
                cachedData ?: return true

                return shouldFetch(
                    requestAccountHash = ethDataManager.accountAddress,
                    cachedAccountHash = cachedData.data.accountHash,
                    dataAgeMillis = Calendar.getInstance().timeInMillis - cachedData.lastFetched
                )
            }
        }
    ),
    Erc20DataSource {

    override fun stream(
        refresh: Boolean
    ): Flow<StoreResponse<Throwable, List<Erc20TokenBalance>>> {
        return stream(
            StoreRequest.Cached(
                forceRefresh = refresh
            )
        ).mapData { it.toDomain() }
    }

    override fun invalidate() {
        println("----- ::invalidate Erc200000")
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "Erc20Store"
    }
}
