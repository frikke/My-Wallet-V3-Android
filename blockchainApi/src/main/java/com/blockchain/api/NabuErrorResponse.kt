package com.blockchain.api

import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.IsoDateSerializer
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
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

class NabuApiException internal constructor(
    message: String,
    private val httpErrorCode: Int,
    private val errorType: String?,
    private val errorCode: Int?,
    private val errorDescription: String?
) : Throwable(message) {

    private constructor(message: String, code: Int) : this(
        message = message,
        httpErrorCode = code,
        errorType = null,
        errorCode = null,
        errorDescription = null
    )

    fun getErrorCode(): NabuErrorCodes = errorCode?.let {
        NabuErrorCodes.fromErrorCode(it)
    } ?: NabuErrorCodes.Unknown

    fun getErrorStatusCode(): NabuErrorStatusCodes = NabuErrorStatusCodes.fromErrorCode(httpErrorCode)

    fun getHttpErrorCode(): Int = httpErrorCode

    fun getErrorType(): NabuErrorTypes = errorType?.let {
        NabuErrorTypes.fromErrorStatus(it)
    } ?: NabuErrorTypes.Unknown

    /**
     * Returns a human-readable error message.
     */
    fun getErrorDescription(): String = errorDescription ?: httpErrorCode.toString()

    // TODO: Replace prefix checking with a proper error code -> needs backend changes
    fun isUserWalletLinkError(): Boolean = getErrorDescription().startsWith(USER_WALLET_LINK_ERROR_PREFIX)

    companion object {
        const val USER_WALLET_LINK_ERROR_PREFIX = "User linked to another wallet"

        fun fromErrorMessageAndCode(message: String, code: Int): NabuApiException =
            NabuApiException(message, code)
    }
}

object NabuApiExceptionFactory {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
            contextual(BigIntSerializer)
            contextual(IsoDateSerializer)
        }
    }

    fun fromResponseBody(exception: HttpException): NabuApiException {
        return exception.response()?.errorBody()?.string()?.let { errorBody ->
            val errorResponse = try {
                json.decodeFromString<NabuErrorResponse>(errorBody)
            } catch (ex: Exception) {
                null
            }
            errorResponse?.let {
                val httpErrorCode = exception.code()
                val errorType = it.type
                val errorDescription = it.description
                val errorCode = it.code
                val path = exception.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ")

                NabuApiException(
                    "$httpErrorCode: $errorType - $errorDescription - $errorCode - $path",
                    httpErrorCode,
                    errorType,
                    errorCode,
                    errorDescription
                )
            }
        } ?: NabuApiException.fromErrorMessageAndCode(exception.message(), exception.code())
    }
}

fun Throwable.isInternetConnectionError(): Boolean =
    this is SocketTimeoutException ||
        this is IOException
