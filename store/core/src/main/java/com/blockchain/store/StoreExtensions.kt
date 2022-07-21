package com.blockchain.store

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle

suspend fun <E, T> Flow<StoreResponse<E, T>>.firstOutcome(): Outcome<E, T> =
    mapNotNull {
        when (it) {
            StoreResponse.Loading -> null
            is StoreResponse.Data -> Outcome.Success(it.data)
            is StoreResponse.Error -> Outcome.Failure(it.error)
        }
    }.first()

fun <E, T : Any> Flow<StoreResponse<E, T>>.asSingle(
    errorMapper: (E) -> Throwable,
): Single<T> = rxSingle {
    when (val result = this@asSingle.firstOutcome()) {
        is Outcome.Success -> result.value
        is Outcome.Failure -> throw errorMapper(result.failure)
    }
}

fun <E, T : Any> Flow<StoreResponse<E, T>>.asObservable(
    errorMapper: (E) -> Throwable,
): Observable<T> = filterNot { it is StoreResponse.Loading }
    .asObservable()
    .map { storeResponse ->
        when (storeResponse) {
            is StoreResponse.Data -> storeResponse.data
            is StoreResponse.Error -> throw errorMapper(storeResponse.error)
            StoreResponse.Loading -> throw IllegalStateException()
        }
    }

fun <E, T, R> Flow<StoreResponse<E, T>>.mapData(mapper: (T) -> R): Flow<StoreResponse<E, R>> =
    map {
        when (it) {
            is StoreResponse.Data -> StoreResponse.Data(mapper(it.data))
            is StoreResponse.Error -> it
            is StoreResponse.Loading -> it
        }
    }

fun <E, T, R> Flow<StoreResponse<E, List<T>>>.mapListData(mapper: (T) -> R): Flow<StoreResponse<E, List<R>>> =
    map {
        when (it) {
            is StoreResponse.Data -> StoreResponse.Data(it.data.map(mapper))
            is StoreResponse.Error -> it
            is StoreResponse.Loading -> it
        }
    }

fun <E, T, R> Flow<StoreResponse<E, T>>.mapError(mapper: (E) -> R): Flow<StoreResponse<R, T>> =
    map {
        when (it) {
            is StoreResponse.Data -> it
            is StoreResponse.Error -> StoreResponse.Error(mapper(it.error))
            is StoreResponse.Loading -> it
        }
    }

/**
 * E types will be removed by antonis pr
 */
fun <E, T> Flow<StoreResponse<E, T>>.getDataOrThrow(): Flow<T> =
    filterNot { it is StoreResponse.Loading }
        .map {
            when (it) {
                is StoreResponse.Data -> it.data
                is StoreResponse.Error -> (it.error as? Exception)?.let { throw it } ?: throw Exception("todo")
                is StoreResponse.Loading -> throw IllegalStateException()
            }
        }