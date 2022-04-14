package com.blockchain.store

import app.cash.turbine.test
import com.blockchain.store.impl.RealStore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {

    val testScope = TestScope()

    val fetcher: Fetcher<Key, Throwable, Item> = mockk()

    val cacheReadState = MutableSharedFlow<CachedData<Key, Item>?>(replay = 1)
    val cache: Cache<Key, Item> = mockk {
        every { read(any()) } returns cacheReadState
        coEvery { write(any()) } just Runs
    }
    val mediator: Mediator<Key, Item> = mockk()
    val store: KeyedStore<Key, Throwable, Item> = RealStore(testScope, fetcher, cache, mediator)

    @Test
    fun `fresh request on failure should not get the current cached value but still listen for future updates to cache`() = testScope.runTest {
        val error = IllegalStateException()
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Failure(error)
        cacheReadState.emit(CachedData(KEY, Item(1), 1))

        store.stream(KeyedStoreRequest.Fresh(KEY)).test {
            assertEquals(StoreResponse.Loading, awaitItem())
            assertEquals(StoreResponse.Error(error), awaitItem())
            expectNoEvents()
            val dataFetchedInTheFuture = Item(123)
            cacheReadState.emit(CachedData(KEY, dataFetchedInTheFuture, 123))
            assertEquals(StoreResponse.Data(dataFetchedInTheFuture), awaitItem())
        }
    }

    @Test
    fun `fresh request success`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        cacheReadState.emit(CachedData(KEY, Item(1), 1))

        store.stream(KeyedStoreRequest.Fresh(KEY)).test {
            assertEquals(StoreResponse.Loading, awaitItem())
            assertEquals(StoreResponse.Data(resultData), awaitItem())
            val dataFetchedInTheFuture = Item(3)
            cacheReadState.emit(CachedData(KEY, dataFetchedInTheFuture, 3))
            assertEquals(StoreResponse.Data(dataFetchedInTheFuture), awaitItem())
        }

        coVerify { cache.write(match { it.data == resultData }) }
    }

    @Test
    fun `cached force refresh request network success`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        val cachedItem = Item(1)
        cacheReadState.emit(CachedData(KEY, cachedItem, 1))

        store.stream(KeyedStoreRequest.Cached(KEY, true)).test {
            assertEquals(StoreResponse.Data(cachedItem), awaitItem())
            assertEquals(StoreResponse.Loading, awaitItem())
            // the store should not proactively emit after fetch success, it should rely on the cache to emit a new cached item
            expectNoEvents()

            cacheReadState.emit(CachedData(KEY, resultData, 2))
            assertEquals(StoreResponse.Data(resultData), awaitItem())
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

        store.stream(KeyedStoreRequest.Cached(KEY, true)).test {
            assertEquals(StoreResponse.Data(cachedItem), awaitItem())
            assertEquals(StoreResponse.Loading, awaitItem())
            assertEquals(StoreResponse.Error(error), awaitItem())

            val futureItem = Item(2)
            cacheReadState.emit(CachedData(KEY, futureItem, 2))
            assertEquals(StoreResponse.Data(futureItem), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
    }

    @Test
    fun `cached non refresh request should fetch network success`() = testScope.runTest {
        val resultData = Item(2)
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Success(resultData)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        coEvery { mediator.shouldFetch(cachedData) } returns true
        cacheReadState.emit(cachedData)

        store.stream(KeyedStoreRequest.Cached(KEY, false)).test {
            assertEquals(StoreResponse.Data(cachedItem), awaitItem())
            assertEquals(StoreResponse.Loading, awaitItem())
            // the store should not proactively emit after fetch success, it should rely on the cache to emit a new cached item
            expectNoEvents()

            cacheReadState.emit(CachedData(KEY, resultData, 2))
            assertEquals(StoreResponse.Data(resultData), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
        coVerify { cache.write(match { it.data == resultData }) }
        coVerify { mediator.shouldFetch(cachedData) }
    }

    @Test
    fun `cached non refresh request should fetch network failure`() = testScope.runTest {
        val error = IllegalStateException("error")
        coEvery { fetcher.fetch(KEY) } returns FetcherResult.Failure(error)
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        coEvery { mediator.shouldFetch(cachedData) } returns true
        cacheReadState.emit(cachedData)

        store.stream(KeyedStoreRequest.Cached(KEY, false)).test {
            assertEquals(StoreResponse.Data(cachedItem), awaitItem())
            assertEquals(StoreResponse.Loading, awaitItem())
            assertEquals(StoreResponse.Error(error), awaitItem())

            val futureItem = Item(2)
            cacheReadState.emit(CachedData(KEY, futureItem, 2))
            assertEquals(StoreResponse.Data(futureItem), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) }
    }

    @Test
    fun `cached non refresh should not fetch`() = testScope.runTest {
        val cachedItem = Item(1)
        val cachedData = CachedData(KEY, cachedItem, 1)
        cacheReadState.emit(cachedData)
        every { mediator.shouldFetch(cachedData) } returns false

        store.stream(KeyedStoreRequest.Cached(KEY, false)).test {
            assertEquals(StoreResponse.Data(cachedItem), awaitItem())

            val futureItem = Item(2)
            cacheReadState.emit(CachedData(KEY, futureItem, 2))
            assertEquals(StoreResponse.Data(futureItem), awaitItem())
            expectNoEvents()
        }

        coVerify { fetcher.fetch(KEY) wasNot Called }
        verify { mediator.shouldFetch(cachedData) }
    }
}

data class Key(val value: String)
data class Item(val value: Int)
val KEY = Key("456")