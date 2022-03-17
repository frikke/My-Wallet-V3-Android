package piuk.blockchain.androidcore.utils.extensions

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.rx3.rxSingle

fun <E : Throwable, R : Any> rxSingleOutcome(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Outcome<E, R>
): Single<R> = rxSingle(context) {
    when (val outcome = block(this)) {
        is Outcome.Success -> outcome.value
        is Outcome.Failure -> throw outcome.failure
    }
}
