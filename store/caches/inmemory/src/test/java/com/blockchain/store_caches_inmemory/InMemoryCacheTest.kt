package com.blockchain.store_caches_inmemory

import app.cash.turbine.test
import com.blockchain.store.Cache
import com.blockchain.store.CachedData
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.koin.test.KoinTest

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryCacheTest : KoinTest {

    private val cache: Cache<Key, Item> = InMemoryCache()

    @Test
    fun `reading for the first time should return null`() = runTest {
        cache.read(KEY).test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `writing should emit a new value in read`() = runTest {
        cache.read(KEY).test {
            assertEquals(null, awaitItem())
            val cached1 = CachedData(KEY, Item(123), 1)
            cache.write(cached1)
            assertEquals(cached1, awaitItem())
            val cached2 = CachedData(KEY, Item(223), 2)
            cache.write(cached2)
            assertEquals(cached2, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `writing to a different key should emit a the same value in read`() = runTest {
        cache.read(KEY).test {
            assertEquals(null, awaitItem())
            val cached1 = CachedData(KEY, Item(123), 1)
            cache.write(cached1)
            assertEquals(cached1, awaitItem())
            cache.write(CachedData(KEY2, Item(223), 2))
            assertEquals(cached1, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `marking as stale should emit a new value in read with lastFetched zeroed`() = runTest {
        cache.read(KEY).test {
            assertEquals(null, awaitItem())
            val cached1 = CachedData(KEY, Item(123), 1)
            cache.write(cached1)
            assertEquals(cached1, awaitItem())
            cache.markAsStale(KEY)
            assertEquals(cached1.copy(lastFetched = 0), awaitItem())
            expectNoEvents()
        }
    }
}

data class Key(val value: String)
data class Item(val value: Int)

val KEY = Key("456")
val KEY2 = Key("987")
