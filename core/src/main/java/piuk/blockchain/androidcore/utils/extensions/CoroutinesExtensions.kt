package piuk.blockchain.androidcore.utils.extensions

import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

fun <E, R : Any> rxSingleOutcome(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Outcome<E, R>
): Single<R> = rxSingle(context) {
    block(this).getOrThrow()
}

fun <E, R : Any?> rxMaybeOutcome(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Outcome<E, R?>
): Maybe<R> = rxMaybe(context) {
    block(this).getOrThrow()
}

fun <E, R : Any> rxCompletableOutcome(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Outcome<E, R>
): Completable = rxCompletable(context) {
    block(this).getOrThrow()
}

suspend fun <T : Any> Single<T>.awaitOutcome(): Outcome<Exception, T> =
    try {
        Outcome.Success(await())
    } catch (ex: Exception) {
        if (ex is CancellationException) throw ex
        Outcome.Failure(ex)
    }

suspend fun <T : Any, E : Any> Single<T>.awaitOutcome(errorMapper: (Exception) -> E): Outcome<E, T> =
    try {
        Outcome.Success(await())
    } catch (ex: Exception) {
        if (ex is CancellationException) throw ex
        Outcome.Failure(errorMapper(ex))
    }

suspend fun Completable.awaitOutcome(): Outcome<Exception, Unit> =
    try {
        Outcome.Success(await())
    } catch (ex: Exception) {
        if (ex is CancellationException) throw ex
        Outcome.Failure(ex)
    }
