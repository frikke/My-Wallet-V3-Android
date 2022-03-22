package com.blockchain.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

class NabuApiException internal constructor(message: String) : Throwable(message) {

    internal constructor(
        message: String,
        httpErrorCode: Int,
        error: String,
        errorCode: Int,
        errorDescription: String
    ) : this(message) {
        _httpErrorCode = httpErrorCode
        _error = error
        _errorCode = errorCode
        _errorDescription = errorDescription
    }

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
        const val USER_WALLET_LINK_ERROR_PREFIX = "User linked to another wallet"
    }
}

object NabuApiExceptionFactory : KoinComponent {

    private val json: Json by inject()

    fun fromResponseBody(exception: Throwable?): NabuApiException {
        return if (exception is HttpException) {
            exception.response()?.errorBody()?.string()?.let { errorBody ->
                val errorResponse = try {
                    json.decodeFromString<NabuErrorResponse>(errorBody)
                } catch (ex: Exception) {
                    null
                }
                errorResponse?.let {
                    val httpErrorCode = exception.code()
                    val error = it.type
                    val errorDescription = it.description
                    val errorCode = it.code
                    val path = exception.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ")

                    NabuApiException(
                        "$httpErrorCode: $error - $errorDescription - $errorCode - $path",
                        httpErrorCode,
                        error,
                        errorCode,
                        errorDescription
                    )
                }
            } ?: NabuApiException(exception.message())
        } else {
            NabuApiException(exception?.message ?: "Unknown exception")
        }
    }
}
