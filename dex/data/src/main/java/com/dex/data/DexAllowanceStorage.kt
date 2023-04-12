package com.dex.data

import com.blockchain.api.dex.DexTransactionsApiService
import com.blockchain.api.dex.TokenAllowanceResponse
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import java.math.BigInteger

class DexAllowanceStorage(
    private val apiService: DexTransactionsApiService,
) : KeyedStore<AllowanceKey, TokenAllowanceResponse> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = "DexAllowanceStorage",
    fetcher = Fetcher.Keyed.ofOutcome(
        mapper = { key ->
            apiService.allowance(
                address = key.address,
                currencyContract = key.currencyContract,
                networkSymbol = key.networkSymbol,
            )
        }
    ),
    keySerializer = AllowanceKey.serializer(),
    dataSerializer = TokenAllowanceResponse.serializer(),
    mediator = object : Mediator<AllowanceKey, TokenAllowanceResponse> {
        override fun shouldFetch(cachedData: CachedData<AllowanceKey, TokenAllowanceResponse>?): Boolean {
            return cachedData == null || cachedData.lastFetched == 0L ||
                BigInteger(cachedData.data.result.allowance).compareTo(BigInteger.ZERO) == 0
        }
    }
)

@kotlinx.serialization.Serializable
data class AllowanceKey(
    val address: String,
    val currencyContract: String,
    val networkSymbol: String,
)
