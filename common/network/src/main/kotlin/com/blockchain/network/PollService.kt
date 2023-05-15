package com.blockchain.network

import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

sealed class PollResult<T>(val value: T) {
    class FinalResult<T>(value: T) : PollResult<T>(value)
    class TimeOut<T>(value: T) : PollResult<T>(value)
    class Cancel<T>(value: T) : PollResult<T>(value)
}

class PollService<T : Any>(
    private val fetcher: Single<T>,
    private val matcher: (T) -> Boolean
) {
    val cancel = PublishSubject.create<Boolean>()
    private val isCancelled = AtomicBoolean(false)

    fun start(timerInSec: Long = 5, retries: Int = 20) =
        fetcher.repeatWhen { it.delay(timerInSec, TimeUnit.SECONDS).zipWith(Flowable.range(0, retries)) }
            .toObservable()
            .doOnSubscribe {
                isCancelled.set(false)
            }
            .takeUntil {
                matcher(it)
            }
            .takeUntil(
                cancel.doOnNext {
                    isCancelled.set(true)
                }
            )
            .lastOrError()
            .map { value ->
                when {
                    isCancelled.get() -> PollResult.Cancel(value)
                    matcher(value) -> PollResult.FinalResult(value)
                    else -> PollResult.TimeOut(value)
                }
            }

    companion object {
        suspend fun <T : Any> poll(
            fetch: suspend () -> Outcome<Exception, T>,
            until: (T) -> Boolean,
            timerInSec: Long = 5,
            retries: Int = 20
        ): Outcome<Exception, PollResult<T>> {
            var lastFetched: T? = null
            var currentRetry = 0
            while (currentRetry++ <= retries) {
                when (val result = fetch()) {
                    is Outcome.Success -> {
                        val value = result.value
                        if (until(value)) {
                            return result.map { PollResult.FinalResult(value) }
                        } else {
                            lastFetched = value
                        }
                    }
                    is Outcome.Failure -> return result
                }

                delay(timerInSec * 1000)
            }

            return if (lastFetched != null) {
                Outcome.Success(PollResult.TimeOut(lastFetched))
            } else {
                Outcome.Failure(NoSuchElementException("No result"))
            }
        }
    }
}
