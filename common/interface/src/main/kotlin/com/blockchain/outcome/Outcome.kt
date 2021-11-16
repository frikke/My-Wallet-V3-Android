package com.blockchain.outcome

sealed class Outcome<E, R> {
    data class Success<E, R>(
        val value: R
    ) : Outcome<E, R>()

    data class Failure<E, R>(
        val failure: E
    ) : Outcome<E, R>()
}

fun <E, R, T> Outcome<E, R>.map(transform: (R) -> T): Outcome<E, T> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> Outcome.Failure(failure)
    }
}

// Outcome/Result-type is right-biased by convention. This method allows us to map the type on the left (failure)
fun <E, F, R> Outcome<E, R>.mapLeft(transform: (E) -> F): Outcome<F, R> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(value)
        is Outcome.Failure -> Outcome.Failure(transform(failure))
    }
}

fun <E, R, T> Outcome<E, R>.flatMap(f: (R) -> Outcome<E, T>): Outcome<E, T> =
    when (this) {
        is Outcome.Success -> f(value)
        is Outcome.Failure -> Outcome.Failure(failure)
    }

fun <E, F, R> Outcome<E, R>.flatMapLeft(f: (E) -> Outcome<F, R>): Outcome<F, R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(value)
        is Outcome.Failure -> f(failure)
    }

fun <E, R, T> Outcome<E, R>.fold(onFailure: (E) -> T, onSuccess: (R) -> T): T =
    when (this) {
        is Outcome.Success -> onSuccess(value)
        is Outcome.Failure -> onFailure(failure)
    }
