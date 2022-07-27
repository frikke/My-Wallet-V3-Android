package com.blockchain.api.adapters

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.NabuErrorTypes

sealed class ApiException : Exception() {
    abstract val exception: Exception

    data class KnownError(override val exception: NabuApiException) : ApiException() {
        val errorCode: NabuErrorCodes = exception.getErrorCode()
        val statusCode: NabuErrorStatusCodes = exception.getErrorStatusCode()
        val errorType: NabuErrorTypes = exception.getErrorType()
        val errorDescription: String = exception.getErrorDescription()
    }

    data class HttpError(override val exception: Exception) : ApiException()
    data class NetworkError(override val exception: Exception) : ApiException() // ios,timeouts etc
    data class UnknownApiError(override val exception: Exception) : ApiException() // error code =-1 (5xx)
}
