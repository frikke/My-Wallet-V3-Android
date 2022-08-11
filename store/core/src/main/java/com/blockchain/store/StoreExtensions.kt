package com.blockchain.store

import com.blockchain.data.DataResource
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle

suspend fun <T> Flow<DataResource<T>>.firstOutcome(): Outcome<Exception, T> =
    mapNotNull {
        when (it) {
            DataResource.Loading -> null
            is DataResource.Data -> Outcome.Success(it.data)
            is DataResource.Error -> Outcome.Failure(it.error)
        }
    }.first()

/**
 * todo filter any loading and take first.
 */
fun <T : Any> Flow<DataResource<T>>.asSingle(): Single<T> = rxSingle {
    val first = this@asSingle.filterNot { it is DataResource.Loading }.first()
    when (first) {
        is DataResource.Data -> first.data
        is DataResource.Error -> throw first.error
        is DataResource.Loading -> throw IllegalStateException("Should data or error")
    }
}

fun <T : Any> Flow<DataResource<T>>.asObservable(): Observable<T> = filterNot { it is DataResource.Loading }
    .asObservable()
    .map { storeResponse ->
        when (storeResponse) {
            is DataResource.Data -> storeResponse.data
            is DataResource.Error -> throw storeResponse.error
            DataResource.Loading -> throw IllegalStateException()
        }
    }

fun <T, R> Flow<DataResource<T>>.mapData(mapper: (T) -> R): Flow<DataResource<R>> =
    map {
        when (it) {
            is DataResource.Data -> DataResource.Data(mapper(it.data))
            is DataResource.Error -> it
            is DataResource.Loading -> it
        }
    }

fun <T, R : Exception> Flow<DataResource<T>>.mapError(mapper: (Exception) -> R): Flow<DataResource<T>> =
    map {
        when (it) {
            is DataResource.Data -> it
            is DataResource.Error -> DataResource.Error(mapper(it.error))
            is DataResource.Loading -> it
        }
    }

fun <T, R> Flow<DataResource<List<T>>>.mapListData(mapper: (T) -> R): Flow<DataResource<List<R>>> =
    map {
        when (it) {
            is DataResource.Data -> DataResource.Data(it.data.map(mapper))
            is DataResource.Error -> it
            is DataResource.Loading -> it
        }
    }

fun <T> Flow<DataResource<T>>.getDataOrThrow(): Flow<T> =
    filterNot { it is DataResource.Loading }
        .map {
            when (it) {
                is DataResource.Data -> it.data
                is DataResource.Error ->
                    throw it.error as? Exception
                        ?: it.error as? Throwable
                        ?: Throwable(it.error.toString())
                is DataResource.Loading -> throw IllegalStateException()
            }
        }
