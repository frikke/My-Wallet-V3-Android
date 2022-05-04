package piuk.blockchain.androidcore.utils.extensions

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

fun <E, R : Any> rxSingleOutcome(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Outcome<E, R>
): Single<R> = rxSingle(context) {
    when (val outcome = block(this)) {
        is Outcome.Success -> outcome.value
        is Outcome.Failure -> throw (outcome.failure as? Throwable ?: Exception())
    }
}

fun <E, R : Any?> rxMaybeOutcome(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Outcome<E, R?>
): Maybe<R> = rxMaybe(context) {
    when (val outcome = block(this)) {
        is Outcome.Success -> outcome.value
        is Outcome.Failure -> throw (outcome.failure as? Throwable ?: Exception())
    }
}

suspend fun <T : Any> Single<T>.awaitOutcome(): Outcome<Exception, T> =
    try {
        Outcome.Success(await())
    } catch (ex: Exception) {
        Outcome.Failure(ex)
    }

suspend fun Completable.awaitOutcome(): Outcome<Exception, Unit> =
    try {
        Outcome.Success(await())
    } catch (ex: Exception) {
        Outcome.Failure(ex)
    }
