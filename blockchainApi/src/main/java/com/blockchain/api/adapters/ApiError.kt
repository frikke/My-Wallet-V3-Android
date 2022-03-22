package com.blockchain.api.adapters

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.NabuErrorTypes

sealed class ApiError(open val throwable: Throwable) {
    data class KnownError(override val throwable: NabuApiException) : ApiError(throwable) {
        val errorCode: NabuErrorCodes = throwable.getErrorCode()
        val statusCode: NabuErrorStatusCodes = throwable.getErrorStatusCode()
        val errorType: NabuErrorTypes = throwable.getErrorType()
        val errorDescription: String = throwable.getErrorDescription()
    }
    data class HttpError(override val throwable: Throwable) : ApiError(throwable)
    data class NetworkError(override val throwable: Throwable) : ApiError(throwable)
    data class UnknownApiError(override val throwable: Throwable) : ApiError(throwable)
}
