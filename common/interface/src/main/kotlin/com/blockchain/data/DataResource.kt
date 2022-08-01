package com.blockchain.data

/**
 * [Loading] : emitted exclusively when fetching from network, the next emitted Data or Error will be related to the network fetch and mean that Store is no longer Loading
 * [Data] : emitted when the fetcher completes successfully or when we get a Cached value
 * [Error] : emitted exclusively when fetching from network, when a Fetcher error has occurred
 */
sealed class DataResource<out T> {
    object Loading : DataResource<Nothing>()
    data class Data<out T>(val data: T) : DataResource<T>()
    data class Error(val error: Exception) : DataResource<Nothing>()
}
