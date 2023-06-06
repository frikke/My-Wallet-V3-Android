package com.blockchain.store

import app.cash.turbine.test
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightCache
import com.blockchain.store_caches_persistedjsonsqldelight.SqlDelightStoreIdScopedPersister
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheTest {

    private val persister: SqlDelightStoreIdScopedPersister = mockk()
    private val keyParser: Parser<Key> = mockk()
    private val dataParser: Parser<Item> = mockk()

    private val cache: Cache<Key, Item> = PersistedJsonSqlDelightCache(persister, keyParser, dataParser)

    @Test
    fun `read`() = runTest {
        val persisterData = PersisterData("{key}", "{data}", 1)
        val persisterReadStream = MutableStateFlow(persisterData)
        coEvery { keyParser.encode(KEY) } returns "{key}"
        coEvery { persister.read("{key}") } returns persisterReadStream
        coEvery { dataParser.decode("{data}") } returns Item(123)

        cache.read(KEY).test {
            assertEquals(CachedData(KEY, Item(123), 1), awaitItem())

            coEvery { dataParser.decode("{data300}") } returns Item(300)
            persisterReadStream.value = PersisterData("{key}", "{data300}", 200)
            coVerify { dataParser.decode("{data300}") }

            assertEquals(CachedData(KEY, Item(300), 200), awaitItem())
        }

        coVerify { keyParser.encode(KEY) }
        coVerify { persister.read("{key}") }
        coVerify { dataParser.decode("{data}") }
    }

    @Test
    fun `write`() = runTest {
        val data = Item(300)
        val lastFetched = 200L
        coEvery { keyParser.encode(KEY) } returns "{key}"
        coEvery { dataParser.encode(data) } returns "{data}"
        coEvery { persister.write(any()) } returns Unit

        cache.write(CachedData(KEY, data, lastFetched))

        coVerify { keyParser.encode(KEY) }
        coVerify { dataParser.encode(data) }
        coVerify { persister.write(PersisterData("{key}", "{data}", lastFetched)) }
    }

    @Test
    fun `mark as stale`() = runTest {
        coEvery { keyParser.encode(KEY) } returns "{key}"
        coEvery { persister.markAsStale("{key}") } returns Unit

        cache.markAsStale(KEY)

        coVerify { keyParser.encode(KEY) }
        coVerify { persister.markAsStale("{key}") }
    }
}

data class Key(val value: String)
data class Item(val value: Int)

val KEY = Key("456")
