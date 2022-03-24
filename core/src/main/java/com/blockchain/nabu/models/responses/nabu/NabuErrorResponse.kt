package com.blockchain.nabu.models.responses.nabu

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class NabuErrorResponse(
    /**
     * Machine-readable error code.
     */
    val code: Int = 0,
    /**
     * Machine-readable error type./ο-
     */
    val type: String = "",
    /**
     * Human-readable error description.
     */
    val description: String = ""
)

class NabuApiException private constructor(message: String) : Throwable(message) {

    private var _httpErrorCode: Int = -1
    private var _errorCode: Int = -1
    private lateinit var _error: String
    private lateinit var _errorDescription: String

    fun getErrorCode(): NabuErrorCodes = NabuErrorCodes.fromErrorCode(_errorCode)

    fun getErrorStatusCode(): NabuErrorStatusCodes = NabuErrorStatusCodes.fromErrorCode(_httpErrorCode)

    fun getErrorType(): NabuErrorTypes = NabuErrorTypes.fromErrorStatus(_error)

    /**
     * Returns a human-readable error message.
     */
    fun getErrorDescription(): String = _errorDescription

    // TODO: Replace prefix checking with a proper error code -> needs backend changes
    fun isUserWalletLinkError(): Boolean = getErrorDescription().startsWith(USER_WALLET_LINK_ERROR_PREFIX)

    companion object {
        @SuppressLint("SyntheticAccessor")
        fun fromResponseBody(exception: Throwable?): NabuApiException {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
            }

            return if (exception is HttpException) {
                exception.response()?.errorBody()?.string()?.let { errorBody ->
                    val errorResponse = try {
                        jsonBuilder.decodeFromString<NabuErrorResponse>(errorBody)
                    } catch (e: Exception) {
                        null
                    }
                    errorResponse?.let {
                        val httpErrorCode = exception.code()
                        val error = it.type
                        val errorDescription = it.description
                        val errorCode = it.code
                        val path = exception.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ")

                        NabuApiException("$httpErrorCode: $error - $errorDescription - $errorCode - $path")
                            .apply {
                                _httpErrorCode = httpErrorCode
                                _error = error
                                _errorCode = errorCode
                                _errorDescription = errorDescription
                            }
                    }
                } ?: NabuApiException(exception.message())
            } else {
                NabuApiException(exception?.message ?: "Unknown exception")
            }
        }

        fun withErrorCode(errorCode: Int): NabuApiException {
            return NabuApiException("")
                .apply {
                    _errorCode = errorCode
                }
        }

        const val USER_WALLET_LINK_ERROR_PREFIX = "User linked to another wallet"
    }
}
