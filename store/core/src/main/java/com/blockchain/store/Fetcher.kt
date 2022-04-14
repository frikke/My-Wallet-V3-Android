package com.blockchain.store

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

interface Fetcher<K, E, T> {
    suspend fun fetch(key: K): FetcherResult<E, T>
    
    companion object {
        fun <E, T> of(mapper: suspend () -> FetcherResult<E, T>): Fetcher<Unit, E, T> = object : Fetcher<Unit, E, T> {
            override suspend fun fetch(key: Unit): FetcherResult<E, T> = mapper()
        }

        fun <E, T> ofOutcome(mapper: suspend () -> Outcome<E, T>): Fetcher<Unit, E, T> = object : Fetcher<Unit, E, T> {
            override suspend fun fetch(key: Unit): FetcherResult<E, T> = when (val result = mapper()) {
                is Outcome.Success -> FetcherResult.Success(result.value)
                is Outcome.Failure -> FetcherResult.Failure(result.failure)
            }
        }

        fun <T : Any, E : Any> ofSingle(
            mapper: () -> Single<T>,
            errorMapper: (Throwable) -> E
        ): Fetcher<Unit, E, T> = object : Fetcher<Unit, E, T> {
            override suspend fun fetch(key: Unit): FetcherResult<E, T> =
                mapper()
                    .map { FetcherResult.Success(it) as FetcherResult<E, T> }
                    .onErrorReturn { error -> FetcherResult.Failure(errorMapper(error)) }
                    .await()
        }
    }
    
    object Keyed {
        fun <K, E, T> of(
            mapper: suspend (K) -> FetcherResult<E, T>
        ): Fetcher<K, E, T> = object : Fetcher<K, E, T> {
            override suspend fun fetch(key: K): FetcherResult<E, T> = mapper(key)
        }

        fun <K, E, T> ofOutcome(mapper: suspend (K) -> Outcome<E, T>): Fetcher<K, E, T> = object : Fetcher<K, E, T> {
            override suspend fun fetch(key: K): FetcherResult<E, T> = when (val result = mapper(key)) {
                is Outcome.Success -> FetcherResult.Success(result.value)
                is Outcome.Failure -> FetcherResult.Failure(result.failure)
            }
        }

        fun <K : Any, T : Any, E : Any> ofSingle(
            mapper: (K) -> Single<T>,
            errorMapper: (Throwable) -> E
        ): Fetcher<K, E, T> = object : Fetcher<K, E, T> {
            override suspend fun fetch(key: K): FetcherResult<E, T> =
                mapper(key)
                    .map { FetcherResult.Success(it) as FetcherResult<E, T> }
                    .onErrorReturn { error -> FetcherResult.Failure(errorMapper(error)) }
                    .await()
        }
    }
}

sealed class FetcherResult<out E, out T> {
    data class Success<T>(val value: T) : FetcherResult<Nothing, T>()
    data class Failure<E>(val error: E) : FetcherResult<E, Nothing>()
}