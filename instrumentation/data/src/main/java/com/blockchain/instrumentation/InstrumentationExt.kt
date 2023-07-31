package com.blockchain.instrumentation

import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.Logger
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Example usage:
 *
 * ```
 * interface Repository {
 *     suspend fun method(): Outcome<Exception, Something> = instrument(
 *         "success" to Outcome.Success(Something("bla")),
 *         "failure" to Outcome.Failure(Exception("error")),
 *     )
 *
 *     fun method(): Flow<Something> = instrumentFlow(
 *         "success" to flowOf(Something("bla")),
 *         "failure" to flow { throw Exception("error") },
 *     )
 *
 *     fun method(): Single<Something> = instrumentSingle(
 *         "success" to Single.just(Something("bla")),
 *         "failure" to Single.error(Exception("error")),
 *     )
 * }
 * ```
 */
suspend fun <T> instrument(vararg responses: Pair<String, T>, fallback: (suspend () -> T)? = null): T {
    val environmentConfig = getKoinInstance<EnvironmentConfig>()
    if (!environmentConfig.isRunningInDebugMode()) {
        require(fallback != null)
        return fallback()
    }

    val stackTrace = Exception().stackTrace
    val previousCallSiteIndex = stackTrace.indexOfLast {
        it.toString().contains("com.blockchain.instrumentation.InstrumentationExt")
    }
    val callSite = stackTrace[previousCallSiteIndex + 1].methodName
    val requestId = UUID.randomUUID()
    val instrumentedResponses = responses.map { (key, model) ->
        InstrumentedResponse.Model(key, model)
    }
    InstrumentationQueue.add(requestId, callSite, canPassThrough = fallback != null, instrumentedResponses)

    // If the current coroutine is cancelled this firstOrNull will throw a CancellationException as expected with coroutines
    // but we want to ensure that in this case we remove this requestId from the Queue, hence the try finally block.
    val myInstrumentedRequest = try {
        InstrumentationQueue.queue
            .map { queue -> queue.find { it.requestId == requestId } }
            .firstOrNull { myRequest ->
                if (myRequest == null) {
                    Logger.e("NULL REQUEST")
                    true
                } else {
                    myRequest.pickedResponse != null
                }
            }
    } finally {
        InstrumentationQueue.remove(requestId)
    }

    @Suppress("UNCHECKED_CAST")
    val pickedInstrumentedResponse =
        myInstrumentedRequest?.pickedResponse?.orElse(null) as? InstrumentedResponse.Model<T>
    return pickedInstrumentedResponse?.model ?: fallback!!.invoke()
}

fun <T : Any> instrumentFlow(
    vararg responses: Pair<String, Flow<T>>,
    fallback: (() -> Flow<T>)? = null
): Flow<T> {
    val fallbackSuspend: (suspend () -> Flow<T>)? = fallback?.let {
        { it() }
    }
    return flow {
        emitAll(instrument(*responses, fallback = fallbackSuspend))
    }
}

fun <T : Any> instrumentSingle(
    vararg responses: Pair<String, Single<T>>,
    fallback: (() -> Single<T>)? = null
): Single<T> {
    val fallbackSuspend: (suspend () -> Single<T>)? = fallback?.let {
        { it() }
    }
    val pickedResponseSingle = rxSingle { instrument(*responses, fallback = fallbackSuspend) }
    return pickedResponseSingle.flatMap { it }
}

fun <T : Any> instrumentMaybe(
    vararg responses: Pair<String, Maybe<T>>,
    fallback: (() -> Maybe<T>)? = null
): Maybe<T> {
    val fallbackSuspend: (suspend () -> Maybe<T>)? = fallback?.let {
        { it() }
    }
    val pickedResponseSingle = rxMaybe { instrument(*responses, fallback = fallbackSuspend) }
    return pickedResponseSingle.flatMap { it }
}

private inline fun <reified T : Any> getKoinInstance(): T {
    return object : KoinComponent {
        val value: T by inject()
    }.value
}
