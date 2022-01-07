package com.blockchain.api.adapters

sealed class ApiError(open val throwable: Throwable) {
    data class HttpError(override val throwable: Throwable) : ApiError(throwable)
    data class NetworkError(override val throwable: Throwable) : ApiError(throwable)
    data class UnknownApiError(override val throwable: Throwable) : ApiError(throwable)
}
