package com.blockchain.store

interface Mediator<K, T> {
    fun shouldFetch(cachedData: CachedData<K, T>?): Boolean
}
