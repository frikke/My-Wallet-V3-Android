package com.blockchain.store

import com.blockchain.store.impl.CurrentTimeProvider
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import io.mockk.every
import io.mockk.mockkObject
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class FreshnessMediatorTest {

    val freshness = Freshness.ofMinutes(20)
    val mediator = FreshnessMediator<Key, List<Item>>(freshness)

    @Test
    fun `given null cachedData it should fetch`() {
        val shouldFetch = mediator.shouldFetch(null)
        shouldFetch `should be equal to` true
    }

    @Test
    fun `given empty list cachedData it should fetch`() {
        val cachedData = CachedData<Key, List<Item>>(KEY, emptyList(), 0L)
        val shouldFetch = mediator.shouldFetch(cachedData)
        shouldFetch `should be equal to` true
    }

    @Test
    fun `given last fetched is earlier than current time it should fetch`() {
        // This means that the phone clock was changed
        mockkObject(CurrentTimeProvider)
        val currentTime = 10_000_000L
        every { CurrentTimeProvider.currentTimeMillis() } returns currentTime

        val lastFetched = 20_000_000L

        val cachedData = CachedData<Key, List<Item>>(KEY, emptyList(), lastFetched)
        val shouldFetch = mediator.shouldFetch(cachedData)
        shouldFetch `should be equal to` true
    }

    @Test
    fun `given last fetched older than the freshness threshold it should fetch`() {
        mockkObject(CurrentTimeProvider)
        val currentTime = 10_000_000L
        every { CurrentTimeProvider.currentTimeMillis() } returns currentTime

        val lastFetched = currentTime - freshness.toMillis() - 1L

        val cachedData = CachedData<Key, List<Item>>(KEY, emptyList(), lastFetched)
        val shouldFetch = mediator.shouldFetch(cachedData)
        shouldFetch `should be equal to` true
    }

}