package com.blockchain.store.impl

import com.blockchain.store.CachedData
import com.blockchain.store.Mediator
import com.blockchain.utils.CurrentTimeProvider
import java.util.concurrent.TimeUnit

class FreshnessMediator<K, T>(
    private val freshness: Freshness,
) : Mediator<K, T> {
    override fun shouldFetch(cachedData: CachedData<K, T>?): Boolean {
        val now = CurrentTimeProvider.currentTimeMillis()
        val lastFetchedTimestamp = cachedData?.lastFetched ?: 0L

        return cachedData == null
            || (cachedData is Collection<*> && cachedData.isEmpty())
            // The phone clock was changed
            || now < lastFetchedTimestamp
            || now > lastFetchedTimestamp + freshness.toMillis()
    }
}

data class Freshness private constructor(
    private val amount: Long,
    private val unit: TimeUnit,
) {

    fun toMillis() = unit.toMillis(amount)

    companion object {
        fun ofSeconds(seconds: Long) = Freshness(seconds, TimeUnit.SECONDS)
        fun ofMinutes(minutes: Long) = Freshness(minutes, TimeUnit.MINUTES)
        fun ofHours(hours: Long) = Freshness(hours, TimeUnit.HOURS)
        val DURATION_24_HOURS = ofHours(24L)
        val DURATION_1_HOUR = ofHours(1L)
    }
}