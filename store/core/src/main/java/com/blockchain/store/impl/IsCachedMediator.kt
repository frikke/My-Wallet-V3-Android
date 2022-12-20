package com.blockchain.store.impl

import com.blockchain.store.CachedData
import com.blockchain.store.Mediator

class IsCachedMediator<K, T> : Mediator<K, T> {
    override fun shouldFetch(cachedData: CachedData<K, T>?): Boolean =
        cachedData == null || cachedData.lastFetched == 0L
}
