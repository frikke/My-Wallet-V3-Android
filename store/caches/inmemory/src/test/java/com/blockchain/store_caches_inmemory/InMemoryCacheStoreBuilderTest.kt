package com.blockchain.store_caches_inmemory

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.outcome.Outcome
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryCacheStoreBuilderTest {

    @Test
    fun `building a store with the same id twice should get the same instance of the cache already created instead of creating a new cache`() =
        runTest {
            val cachedItem = Item(137)
            val store1 = InMemoryCacheStoreBuilder().build(
                storeId = "STORE_ID_1",
                fetcher = Fetcher.ofOutcome { Outcome.Success(cachedItem) },
                mediator = object : Mediator<Unit, Item> {
                    override fun shouldFetch(cachedData: CachedData<Unit, Item>?): Boolean = false
                }
            )
            store1.stream(FreshnessStrategy.Fresh).firstOutcome()

            val store2 = InMemoryCacheStoreBuilder().build(
                storeId = "STORE_ID_1",
                fetcher = Fetcher.ofOutcome { Outcome.Success(Item(1)) },
                mediator = object : Mediator<Unit, Item> {
                    override fun shouldFetch(cachedData: CachedData<Unit, Item>?): Boolean = false
                }
            )
            val result = store2.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).firstOutcome()
            assertTrue(result is Outcome.Success && result.value == cachedItem)
        }
}
