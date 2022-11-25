package com.blockchain.outcome

import com.blockchain.data.DataResource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

sealed class Outcome<out E, out R> {
    data class Success<R>(
        val value: R
    ) : Outcome<Nothing, R>()

    data class Failure<E>(
        val failure: E
    ) : Outcome<E, Nothing>()
}

fun <E, R, T> Outcome<E, R>.map(transform: (R) -> T): Outcome<E, T> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }
}

fun <E, F, R> Outcome<E, R>.mapError(transform: (E) -> F): Outcome<F, R> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(value)
        is Outcome.Failure -> Outcome.Failure(transform(failure))
    }
}

suspend fun <E, R, T> Outcome<E, R>.flatMap(f: suspend (R) -> Outcome<E, T>): Outcome<E, T> =
    when (this) {
        is Outcome.Success -> f(value)
        is Outcome.Failure -> Outcome.Failure(failure)
    }

suspend fun <E, F, R> Outcome<E, R>.flatMapLeft(f: suspend (E) -> Outcome<F, R>): Outcome<F, R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(value)
        is Outcome.Failure -> f(failure)
    }

fun <E, R> Outcome<E, R>.doOnSuccess(f: (R) -> Unit): Outcome<E, R> =
    this.also {
        if (this is Outcome.Success) f(this.value)
    }

fun <E, R> Outcome<E, R>.doOnFailure(f: (E) -> Unit): Outcome<E, R> =
    this.also {
        if (this is Outcome.Failure) f(this.failure)
    }

fun <E, R, T> Outcome<E, R>.fold(onFailure: (E) -> T, onSuccess: (R) -> T): T =
    when (this) {
        is Outcome.Success -> onSuccess(value)
        is Outcome.Failure -> onFailure(failure)
    }

fun <E, R> Outcome<E, R>.getOrDefault(defaultValue: R): R =
    when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> defaultValue
    }

fun <E, R> Outcome<E, R>.getOrNull(): R? = getOrDefault(null)

fun <E, R> Outcome<E, R>.getOrElse(onFailure: (E) -> R): R =
    when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> onFailure(failure)
    }

fun <E, R> Outcome<E, R>.getOrThrow(): R =
    when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw (failure as? Exception ?: Exception())
    }

fun <E, T> List<Outcome<E, T>>.anyError() = any { it is Outcome.Failure }
fun <E, T> List<Outcome<E, T>>.getFirstError() = (first { it is Outcome.Failure } as Outcome.Failure)

suspend fun <E, T1, T2, R> zipOutcomes(
    p1: suspend () -> Outcome<E, T1>,
    p2: suspend () -> Outcome<E, T2>,
    transform: (T1, T2) -> R,
): Outcome<E, R> = coroutineScope {
    val a1 = async { p1() }
    val a2 = async { p2() }
    val r1 = a1.await()
    val r2 = a2.await()
    val results = listOf(r1, r2)

    when {
        results.anyError() -> Outcome.Failure(results.getFirstError().failure)
        else -> {
            r1 as Outcome.Success
            r2 as Outcome.Success

            Outcome.Success(transform(r1.value, r2.value))
        }
    }
}

suspend fun <E, T1, T2> zipOutcomes(
    p1: suspend () -> Outcome<E, T1>,
    p2: suspend () -> Outcome<E, T2>,
): Outcome<E, Pair<T1, T2>> = zipOutcomes(p1, p2) { r1, r2 -> r1 to r2 }

suspend fun <E, T1, T2, T3, R> zipOutcomes(
    p1: suspend () -> Outcome<E, T1>,
    p2: suspend () -> Outcome<E, T2>,
    p3: suspend () -> Outcome<E, T3>,
    transform: (T1, T2, T3) -> R,
): Outcome<E, R> = coroutineScope {
    val a1 = async { p1() }
    val a2 = async { p2() }
    val a3 = async { p3() }
    val r1 = a1.await()
    val r2 = a2.await()
    val r3 = a3.await()
    val results = listOf(r1, r2, r3)

    when {
        results.anyError() -> Outcome.Failure(results.getFirstError().failure)
        else -> {
            r1 as Outcome.Success
            r2 as Outcome.Success
            r3 as Outcome.Success

            Outcome.Success(transform(r1.value, r2.value, r3.value))
        }
    }
}

suspend fun <E, T1, T2, T3> zipOutcomes(
    p1: suspend () -> Outcome<E, T1>,
    p2: suspend () -> Outcome<E, T2>,
    p3: suspend () -> Outcome<E, T3>,
): Outcome<E, Triple<T1, T2, T3>> = zipOutcomes(p1, p2, p3) { r1, r2, r3 -> Triple(r1, r2, r3) }

fun <E, R> Outcome<E, R>.toDataResource(): DataResource<R> {
    return when (this) {
        is Outcome.Success -> DataResource.Data(value)
        is Outcome.Failure -> DataResource.Error(
            when (failure) {
                is Exception -> failure
                is Throwable -> Exception(failure)
                else -> Exception(failure.toString())
            }
        )
    }
}
