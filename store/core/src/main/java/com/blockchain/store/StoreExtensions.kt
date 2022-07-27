package com.blockchain.store

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle

suspend fun <T> Flow<StoreResponse<T>>.firstOutcome(): Outcome<Exception, T> =
    mapNotNull {
        when (it) {
            StoreResponse.Loading -> null
            is StoreResponse.Data -> Outcome.Success(it.data)
            is StoreResponse.Error -> Outcome.Failure(it.error)
        }
    }.first()

/**
 * todo filter any loading and take first.
 */
fun <T : Any> Flow<StoreResponse<T>>.asSingle(): Single<T> = rxSingle {
    val first = this@asSingle.filterNot { it is StoreResponse.Loading }.first()
    when (first) {
        is StoreResponse.Data -> first.data
        is StoreResponse.Error -> throw first.error
        is StoreResponse.Loading -> throw IllegalStateException("Should data or error")
    }
}

fun <T : Any> Flow<StoreResponse<T>>.asObservable(): Observable<T> = filterNot { it is StoreResponse.Loading }
    .asObservable()
    .map { storeResponse ->
        when (storeResponse) {
            is StoreResponse.Data -> storeResponse.data
            is StoreResponse.Error -> throw storeResponse.error
            StoreResponse.Loading -> throw IllegalStateException()
        }
    }

fun <T, R> Flow<StoreResponse<T>>.mapData(mapper: (T) -> R): Flow<StoreResponse<R>> =
    map {
        when (it) {
            is StoreResponse.Data -> StoreResponse.Data(mapper(it.data))
            is StoreResponse.Error -> it
            is StoreResponse.Loading -> it
        }
    }

fun <T, R> Flow<StoreResponse<List<T>>>.mapListData(mapper: (T) -> R): Flow<StoreResponse<List<R>>> =
    map {
        when (it) {
            is StoreResponse.Data -> StoreResponse.Data(it.data.map(mapper))
            is StoreResponse.Error -> it
            is StoreResponse.Loading -> it
        }
    }

/*
fun <T, R> Flow<StoreResponse<T>>.mapError(mapper: (Exception) -> R): Flow<StoreResponse<R, T>> =
    map {
        when (it) {
            is StoreResponse.Data -> it
            is StoreResponse.Error -> StoreResponse.Error(mapper(it.error))
            is StoreResponse.Loading -> it
        }
<<<<<<< HEAD
    }*/


fun <T> Flow<StoreResponse< T>>.getDataOrThrow(): Flow<T> =
    filterNot { it is StoreResponse.Loading }
        .map {
            when (it) {
                is StoreResponse.Data -> it.data
                is StoreResponse.Error -> throw it.error as? Exception
                    ?: it.error as? Throwable
                    ?: Throwable(it.error.toString())
                is StoreResponse.Loading -> throw IllegalStateException()
            }
        }
