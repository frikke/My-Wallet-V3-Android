package com.blockchain.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

fun <T> DataResource<T>.doOnLoading(f: () -> Unit): DataResource<T> {
    return also {
        if (this is DataResource.Loading) f()
    }
}

fun <T> DataResource<T>.doOnData(f: (T) -> Unit): DataResource<T> {
    return also {
        if (this is DataResource.Data) f(this.data)
    }
}

fun <T> Flow<DataResource<T>>.doOnData(f: suspend (T) -> Unit): Flow<DataResource<T>> {
    return map { dataResource ->
        dataResource.also {
            if (it is DataResource.Data) f(it.data)
        }
    }
}

fun <T> DataResource<T>.doOnFailure(f: (Exception) -> Unit): DataResource<T> {
    return also {
        if (this is DataResource.Error) f(this.error)
    }
}

fun <T> List<DataResource<T>>.anyLoading() = any { it is DataResource.Loading }
fun <T> List<DataResource<T>>.anyError() = any { it is DataResource.Error }
fun <T> List<DataResource<T>>.getFirstError() = (first { it is DataResource.Error } as DataResource.Error)

fun <T> Flow<DataResource<T>>.onErrorReturn(errorToData: (Exception) -> T): Flow<DataResource<T>> {
    return map { dataResource ->
        when (dataResource) {
            is DataResource.Error -> {
                DataResource.Data(errorToData(dataResource.error))
            }
            else -> dataResource
        }
    }
}

fun <T1, T2, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    transform: (T1, T2) -> R
): DataResource<R> {
    val results = listOf(r1, r2)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data))
        }
    }
}

fun <T1, T2, T3, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    transform: (T1, T2, T3) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data))
        }
    }
}

fun <T1, T2, T3, T4, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    transform: (T1, T2, T3, T4) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data))
        }
    }
}

fun <T1, T2, T3, T4, T5, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    r5: DataResource<T5>,
    transform: (T1, T2, T3, T4, T5) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4, r5)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data
            r5 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data, r5.data))
        }
    }
}

fun <T, R> combineDataResources(
    dataResources: Iterable<DataResource<T>>,
    transform: (List<T>) -> R
): DataResource<R> {
    val results = dataResources.toList()

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            DataResource.Data(transform(results.map { (it as DataResource.Data).data }))
        }
    }
}
