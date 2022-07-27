package com.blockchain.store

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

interface Fetcher<in K, out T> {
    suspend fun fetch(key: K): FetcherResult<T>

    companion object {
        fun <E, T> of(mapper: suspend () -> FetcherResult<T>): Fetcher<Unit, T> = object : Fetcher<Unit, T> {
            override suspend fun fetch(key: Unit): FetcherResult<T> = mapper()
        }

        fun <T> ofOutcome(mapper: suspend () -> Outcome<Exception, T>): Fetcher<Unit, T> =
            object : Fetcher<Unit, T> {
                override suspend fun fetch(key: Unit): FetcherResult<T> = when (val result = mapper()) {
                    is Outcome.Success -> FetcherResult.Success(result.value)
                    is Outcome.Failure -> FetcherResult.Failure(result.failure)
                }
            }

        fun <T : Any> ofSingle(
            mapper: () -> Single<T>
        ): Fetcher<Unit, T> = object : Fetcher<Unit, T> {
            override suspend fun fetch(key: Unit): FetcherResult<T> =
                mapper()
                    .map { FetcherResult.Success(it) as FetcherResult<T> }
                    .onErrorReturn { error ->
                        (error as? Exception)?.let {
                            FetcherResult.Failure(it)
                        } ?: throw error
                    }
                    .await()
        }
    }

    object Keyed {
        fun <K, T> of(
            mapper: suspend (K) -> FetcherResult<T>
        ): Fetcher<K, T> = object : Fetcher<K, T> {
            override suspend fun fetch(key: K): FetcherResult<T> = mapper(key)
        }

        fun <K, T> ofOutcome(mapper: suspend (K) -> Outcome<Exception, T>): Fetcher<K, T> = object : Fetcher<K, T> {
            override suspend fun fetch(key: K): FetcherResult<T> = when (val result = mapper(key)) {
                is Outcome.Success -> FetcherResult.Success(result.value)
                is Outcome.Failure -> FetcherResult.Failure(result.failure)
            }
        }

        fun <K : Any, T : Any> ofSingle(
            mapper: (K) -> Single<T>
        ): Fetcher<K, T> = object : Fetcher<K, T> {
            override suspend fun fetch(key: K): FetcherResult<T> =
                mapper(key)
                    .map { FetcherResult.Success(it) as FetcherResult<T> }
                    .onErrorReturn { error ->
                        (error as? Exception)?.let {
                            FetcherResult.Failure(it)
                        } ?: throw error
                    }
                    .await()
        }
    }
}

sealed class FetcherResult<out T> {
    data class Success<out T>(val value: T) : FetcherResult<T>()
    data class Failure(val error: Exception) : FetcherResult<Nothing>()
}