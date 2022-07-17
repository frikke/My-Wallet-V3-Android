package com.blockchain.api.adapters

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.NabuErrorTypes

sealed class ApiError(open val exception: Exception) {
    data class KnownError(override val exception: NabuApiException) : ApiError(exception) {
        val errorCode: NabuErrorCodes = exception.getErrorCode()
        val statusCode: NabuErrorStatusCodes = exception.getErrorStatusCode()
        val errorType: NabuErrorTypes = exception.getErrorType()
        val errorDescription: String = exception.getErrorDescription()
    }
    data class HttpError(override val exception: Exception) : ApiError(exception)
    data class NetworkError(override val exception: Exception) : ApiError(exception)
    data class UnknownApiError(override val exception: Exception) : ApiError(exception)
}
