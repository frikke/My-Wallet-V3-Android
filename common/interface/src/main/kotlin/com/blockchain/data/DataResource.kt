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

fun <T> List<DataResource<T>>.anyLoading() = any { it is DataResource.Loading }
fun <T> List<DataResource<T>>.anyError() = any { it is DataResource.Error }
fun <T> List<DataResource<T>>.getFirstError() = (first { it is DataResource.Error } as DataResource.Error)
