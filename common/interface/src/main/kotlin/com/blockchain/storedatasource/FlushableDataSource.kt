package com.blockchain.storedatasource

interface FlushableDataSource {
    fun invalidate()
}

interface KeyedFlushableDataSource<T> {
    fun invalidate(param: T)
}
