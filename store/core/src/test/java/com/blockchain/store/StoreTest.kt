package com.blockchain.store

import app.cash.turbine.test
import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.internalnotifications.NotificationReceiver
import com.blockchain.store.impl.RealStore
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {

    private val testScope = TestScope()

    private val fetcher: Fetcher<Key, Item> = mockk()
    private val emitter: NotificationReceiver = mockk {
        coEvery { events } returns flowOf()
    }

    private val cacheReadState = MutableSharedFlow<CachedData<Key, Item>?>(replay = 1)
    private val cache: Cache<Key, Item> = mockk {
        every { read(any()) } returns cacheReadState
        coEvery { write(any()) } just Runs
    }
    private val mediator: Mediator<Key, Item> = mockk()
    private val store: KeyedStore<Key, Item> =
        RealStore(
            testScope, fetcher,
            cache, mediator = mediator,
            notificationReceiver = emitter,
            reset = CacheConfiguration.default()
        )

    @Test
    fun `fresh request on failure should not get the current cached value but still listen for future updates to cache`() =
        testScope.runTest {
            val error = IllegalStateException()
            coEvery { fetcher.fetch(KEY) } returns FetcherResult.Failure(error)
            cacheReadState.emit(CachedData(KEY, Item(1), 1))

            store.stream(KeyedFreshnessStrategy.Fresh(KEY)).test {
                assertEquals(DataResource.Loading, awaitItem())
                assertEquals(DataResource.Error(error), awaitItem())
                expectNoEvents()
                val dataFetchedInTheFuture = Item(123)
                cacheReadState.emit(CachedData(KEY, dataFetchedInTheFuture, 123))
                assertEquals(DataResource.Data(dataFetchedInTheFuture), awaitItem())
            }
        }

    @Test
    fun `fresh request success`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        cacheReadState.emit(CachedData(KEY, Item(1), 1))

        store.stream(KeyedFreshnessStrategy.Fresh(KEY)).test {
            assertEquals(DataResource.Loading, awaitItem())
            assertEquals(DataResource.Data(resultData), awaitItem())
            val dataFetchedInTheFuture = Item(3)
            cacheReadState.emit(CachedData(KEY, dataFetchedInTheFuture, 3))
            assertEquals(DataResource.Data(dataFetchedInTheFuture), awaitItem())
        }

        coVerify { cache.write(match { it.data == resultData }) }
    }

    @Test
    fun `cached force refresh request network success`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        cacheReadState.emit(cachedData)
        coEvery { mediator.shouldFetch(any()) } returns false

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.ForceRefresh)).test {
            assertEquals(DataResource.Data(cachedItem), awaitItem())
            assertEquals(DataResource.Loading, awaitItem())
            // the store should not proactively emit after fetch success, it should rely on the cache to emit a new cached item
            expectNoEvents()

            cacheReadState.emit(CachedData(KEY, resultData, 2))
            assertEquals(DataResource.Data(resultData), awaitItem())
        }

        coVerify { fetcher.fetch(KEY) }
        coVerify { cache.write(match { it.data == resultData }) }
    }

    @Test
    fun `cached force refresh request network failure`() = testScope.runTest {
        val error = IllegalStateException("error")
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Failure(error)
        val cachedItem = Item(1)
        cacheReadState.emit(CachedData(KEY, cachedItem, 1))
        coEvery { mediator.shouldFetch(any()) } returns false

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.ForceRefresh)).test {
            assertEquals(DataResource.Data(cachedItem), awaitItem())
            assertEquals(DataResource.Loading, awaitItem())
            assertEquals(DataResource.Error(error), awaitItem())

            val futureItem = Item(2)
            cacheReadState.emit(CachedData(KEY, futureItem, 2))
            assertEquals(DataResource.Data(futureItem), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
    }

    @Test
    fun `cached refresh if stale request should fetch network success`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        coEvery { mediator.shouldFetch(cachedData) } returns true
        cacheReadState.emit(cachedData)

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.RefreshIfStale)).test {
            assertEquals(DataResource.Loading, awaitItem())
            // the store should not proactively emit after fetch success, it should rely on the cache to emit a new cached item
            expectNoEvents()

            val cachedResultData = CachedData(KEY, resultData, 2)
            coEvery { mediator.shouldFetch(cachedResultData) } returns true
            cacheReadState.emit(cachedResultData)
            val secondItem = awaitItem()
            assertEquals(DataResource.Data(resultData), secondItem)
            assertTrue(secondItem is DataResource.Data)
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
        coVerify { cache.write(match { it.data == resultData }) }
        coVerify { mediator.shouldFetch(cachedData) }
    }

    @Test
    fun `stale cached data should not be emitted`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        coEvery { mediator.shouldFetch(cachedData) } returns true
        cacheReadState.emit(cachedData)

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.RefreshIfStale)).test {
            assertEquals(DataResource.Loading, awaitItem())
            // the store should not proactively emit after fetch success, it should rely on the cache to emit a new cached item
            expectNoEvents()

            val resultCachedData = CachedData(KEY, resultData, 2)
            coEvery { mediator.shouldFetch(resultCachedData) } returns true
            cacheReadState.emit(resultCachedData)
            val secondItem = awaitItem()
            assertEquals(DataResource.Data(resultData), secondItem)
            assertTrue(secondItem is DataResource.Data)
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
        coVerify { cache.write(match { it.data == resultData }) }
        coVerify { mediator.shouldFetch(cachedData) }
    }

    @Test
    fun `cached refresh if stale request should fetch network failure`() = testScope.runTest {
        val error = IllegalStateException("error")
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Failure(error)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        coEvery { mediator.shouldFetch(cachedData) } returns true
        cacheReadState.emit(cachedData)

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.RefreshIfStale)).test {
            assertEquals(DataResource.Loading, awaitItem())
            assertEquals(DataResource.Error(error), awaitItem())

            val futureItem = Item(2)
            val cachedFutureItem = CachedData(KEY, futureItem, 2)
            coEvery { mediator.shouldFetch(cachedFutureItem) } returns true
            cacheReadState.emit(cachedFutureItem)
            val secondItem = awaitItem()
            assertEquals(DataResource.Data(futureItem), secondItem)
            assertTrue(secondItem is DataResource.Data)
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
    }

    @Test
    fun `cached refresh if stale should not fetch`() = testScope.runTest {
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        cacheReadState.emit(cachedData)
        every { mediator.shouldFetch(any()) } returns false

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.RefreshIfStale)).test {
            assertEquals(DataResource.Data(cachedItem), awaitItem())

            val futureItem = Item(2)
            cacheReadState.emit(CachedData(KEY, futureItem, 2))
            assertEquals(DataResource.Data(futureItem), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) wasNot Called }
        verify { mediator.shouldFetch(cachedData) }
    }

    @Test
    fun `cached refresh if older than request should fetch network`() = testScope.runTest {
        val refreshStrategy = RefreshStrategy.RefreshIfOlderThan(1, TimeUnit.MINUTES)

        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        coEvery { mediator.shouldFetch(cachedData) } returns false
        cacheReadState.emit(cachedData)

        store.stream(KeyedFreshnessStrategy.Cached(KEY, refreshStrategy)).test {
            val firstItem = awaitItem()
            assertEquals(DataResource.Data(cachedItem), firstItem)
            assertTrue(firstItem is DataResource.Data)

            assertEquals(DataResource.Loading, awaitItem())
            // the store should not proactively emit after fetch success, it should rely on the cache to emit a new cached item
            expectNoEvents()

            val cachedResultData = CachedData(KEY, resultData, 2)
            coEvery { mediator.shouldFetch(cachedResultData) } returns false
            cacheReadState.emit(cachedResultData)
            val secondItem = awaitItem()
            assertEquals(DataResource.Data(resultData), secondItem)
            assertTrue(secondItem is DataResource.Data)
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
        coVerify { cache.write(match { it.data == resultData }) }
        coVerify { mediator.shouldFetch(cachedData) }
    }

    @Test
    fun `cached refresh if older than should not fetch`() = testScope.runTest {
        val refreshStrategy = RefreshStrategy.RefreshIfOlderThan(1, TimeUnit.MINUTES)
        val now = System.currentTimeMillis()

        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, now - 30)
        cacheReadState.emit(cachedData)
        every { mediator.shouldFetch(any()) } returns false

        store.stream(KeyedFreshnessStrategy.Cached(KEY, refreshStrategy)).test {
            assertEquals(DataResource.Data(cachedItem), awaitItem())

            val futureItem = Item(2)
            cacheReadState.emit(CachedData(KEY, futureItem, 2))
            assertEquals(DataResource.Data(futureItem), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) wasNot Called }
        verify { mediator.shouldFetch(cachedData) }
    }

    @Test
    fun `cache should filter out duplicated emissions`() = testScope.runTest {
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        every { mediator.shouldFetch(any()) } returns false

        store.stream(KeyedFreshnessStrategy.Cached(KEY, RefreshStrategy.RefreshIfStale)).test {
            cacheReadState.emit(cachedData)
            assertEquals(DataResource.Data(cachedItem), awaitItem())
            cacheReadState.emit(cachedData)
            expectNoEvents()
        }
    }
}

data class Key(val value: String)
data class Item(val value: Int)

val KEY = Key("456")
