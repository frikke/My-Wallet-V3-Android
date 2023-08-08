package com.blockchain.api

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.lang.RuntimeException
import retrofit2.HttpException

open class ApiException : RuntimeException {

    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    protected constructor(
        message: String,
        cause: Throwable,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}

// This is an interim method, until we move the rest of the Nabu API over to this module
internal fun <T> Single<T>.wrapErrorMessage(): Single<T> = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Single.error(
            ApiException(
                cause = it,
                message = it.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ").orEmpty()
            )
        )

        else -> Single.error(it)
    }
}

internal fun <T> Maybe<T>.wrapErrorMessage(): Maybe<T> = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Maybe.error(
            ApiException(
                cause = it,
                message = it.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ").orEmpty()
            )
        )

        else -> Maybe.error(it)
    }
}

internal fun Completable.wrapErrorMessage(): Completable = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Completable.error(
            ApiException(
                cause = it,
                message = it.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ").orEmpty()
            )
        )

        else -> Completable.error(it)
    }
}
