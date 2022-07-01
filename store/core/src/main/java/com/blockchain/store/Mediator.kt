package com.blockchain.store

interface Mediator<K, T> {
    fun shouldFetch(requestKey: K, cachedData: CachedData<K, T>?): Boolean
}