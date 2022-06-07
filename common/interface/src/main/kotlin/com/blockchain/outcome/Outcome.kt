package com.blockchain.outcome

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
        is Outcome.Failure -> throw (failure as? Throwable ?: Exception())
    }

// TODO(dtverdota): Check back for uptake in end of July
infix fun <E, R, T> Outcome<E, R>.then(transform: (R) -> T): Outcome<E, T> = map(transform)
