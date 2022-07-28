package com.blockchain.data

/**
 * Defines the way that the [Store.stream] will operate:
 *   - [Fresh] will always skip cache at first, fetch and listen for future cache changes: `[Loading, Data/Error(fetcher), Data(future cache change)]`
 *   - [Cached(forceRefresh=true)] will get the latest cache, fetch and listen for future cache changes: `[Data(cache), Loading, Data/Error(fetcher), Data(future cache change)]`
 *   - [Cached(forceRefresh=false)] will get the latest cache, and only fetch if there is no cached data or if the mediator decides to fetch, it will also listen for future cache changes:
 *      - should fetch == true: `[Data(cache), Loading, Data/Error(fetcher), Data(future cache change)]`
 *      - should fetch == false: `[Data(cache), Data(future cache change)]`
 */
sealed class FreshnessStrategy {
    object Fresh : FreshnessStrategy()
    data class Cached(val forceRefresh: Boolean) : FreshnessStrategy()

    companion object {
        fun <K> FreshnessStrategy.withKey(key: K): KeyedFreshnessStrategy<K> {
            return when (this) {
                Fresh -> KeyedFreshnessStrategy.Fresh(key)
                is Cached -> KeyedFreshnessStrategy.Cached(key, this.forceRefresh)
            }
        }
    }
}

/**
 * Keyed version of [FreshnessStrategy]
 * See [FreshnessStrategy]  for more detailed documentation.
 */
sealed class KeyedFreshnessStrategy<out K> {
    data class Fresh<out K>(val key: K) : KeyedFreshnessStrategy<K>()
    data class Cached<out K>(val key: K, val forceRefresh: Boolean) : KeyedFreshnessStrategy<K>()
}
