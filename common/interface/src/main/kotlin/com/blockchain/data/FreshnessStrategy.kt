package com.blockchain.data

import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * Defines the way that the [Store.stream] will operate:
 *   - [Fresh] will always skip cache at first, fetch and listen for future cache changes: `[Loading, Data/Error(fetcher), Data(future cache change)]`
 *   - [Cached(refreshStrategy=RefreshStrategy.ForceRefresh)] will get the latest cache, fetch and listen for future cache changes: `[Data(cache), Loading, Data/Error(fetcher), Data(future cache change)]`
 *   - [Cached(refreshStrategy=RefreshStrategy.RefreshIfStale)] will get the latest cache, and only fetch if there is no cached data or if the mediator decides to fetch, it will also listen for future cache changes:
 *      - should fetch == true: `[Data(cache), Loading, Data/Error(fetcher), Data(future cache change)]`
 *      - should fetch == false: `[Data(cache), Data(future cache change)]`
 *   - [Cached(refreshStrategy=RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))] will get the latest cache, and only fetch if there is no cached data, the mediator decides to fetch or if the cached data is older than the passed time, it will also listen for future cache changes
 */
sealed class FreshnessStrategy : Serializable {
    object Fresh : FreshnessStrategy()
    data class Cached(val refreshStrategy: RefreshStrategy) : FreshnessStrategy()

    companion object {
        fun <K> FreshnessStrategy.withKey(key: K): KeyedFreshnessStrategy<K> {
            return when (this) {
                Fresh -> KeyedFreshnessStrategy.Fresh(key)
                is Cached -> KeyedFreshnessStrategy.Cached(key, this.refreshStrategy)
            }
        }
    }
}

sealed class RefreshStrategy : Serializable {
    object ForceRefresh : RefreshStrategy()
    object RefreshIfStale : RefreshStrategy()
    data class RefreshIfOlderThan(
        val amount: Long,
        val unit: TimeUnit,
    ) : RefreshStrategy() {
        fun toMillis() = unit.toMillis(amount)
    }
}

/**
 * Keyed version of [FreshnessStrategy]
 * See [FreshnessStrategy]  for more detailed documentation.
 */
sealed class KeyedFreshnessStrategy<out K> : Serializable {
    data class Fresh<out K>(val key: K) : KeyedFreshnessStrategy<K>()
    data class Cached<out K>(val key: K, val refreshStrategy: RefreshStrategy) : KeyedFreshnessStrategy<K>()
}
