package com.blockchain.storedatasource

interface FlushableDataSource {
    fun invalidate()
}

interface KeyedFlushableDataSource<T> : FlushableDataSource {
    fun invalidate(param: T)
}
