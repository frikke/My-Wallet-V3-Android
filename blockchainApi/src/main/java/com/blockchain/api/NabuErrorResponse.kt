package com.blockchain.api

import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.IsoDateSerializer
import com.blockchain.serializers.KZonedDateTimeSerializer
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import retrofit2.HttpException

@Serializable
private data class NabuErrorResponse(
    val id: String = "",
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
    val description: String = "",

    /**
     * Server side localised error copy to display
     */
    val ux: NabuUxErrorResponse?
)

@Serializable
data class NabuUxErrorResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("title")
    val title: String,
    @SerialName("message")
    val message: String,
    @SerialName("icon")
    val icon: IconData?,
    @SerialName("actions")
    val actions: List<ActionData>?,
    @SerialName("categories")
    val categories: List<String>?
)

@Serializable
data class IconData(
    @SerialName("url")
    val url: String,
    @SerialName("status")
    val status: StatusData?
)

@Serializable
data class StatusData(
    @SerialName("url")
    val url: String
)

@Serializable
data class ActionData(
    @SerialName("title")
    val title: String,
    @SerialName("url")
    val url: String?
)

fun NabuUxErrorResponse.mapActions(): List<ServerErrorAction> =
    this.actions?.map {
        ServerErrorAction(
            title = it.title,
            deeplinkPath = it.url.orEmpty()
        )
    } ?: emptyList()

class NabuApiException constructor(
    message: String,
    private val httpErrorCode: Int,
    private val errorType: String?,
    private val errorCode: Int?,
    private val errorDescription: String?,
    private val path: String?,
    private val id: String?,
    private val serverSideUxError: ServerSideUxErrorInfo?
) : RuntimeException(message) {

    private constructor(message: String, code: Int) : this(
        message = message,
        httpErrorCode = code,
        errorType = null,
        errorCode = null,
        errorDescription = null,
        path = null,
        id = null,
        serverSideUxError = null
    )

    fun getPath(): String = path.orEmpty()

    fun getId(): String = id.orEmpty()

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

    fun getServerSideErrorInfo(): ServerSideUxErrorInfo? = serverSideUxError

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
            contextual(KZonedDateTimeSerializer)
        }
    }

    fun fromServerSideError(uxErrorResponse: NabuUxErrorResponse) =
        NabuApiException(
            message = uxErrorResponse.title,
            httpErrorCode = HttpStatus.OK,
            errorType = null,
            errorCode = null,
            errorDescription = null,
            path = null,
            id = null,
            serverSideUxError = ServerSideUxErrorInfo(
                id = uxErrorResponse.id,
                title = uxErrorResponse.title,
                description = uxErrorResponse.message,
                iconUrl = uxErrorResponse.icon?.url.orEmpty(),
                statusUrl = uxErrorResponse.icon?.status?.url.orEmpty(),
                actions = uxErrorResponse.mapActions(),
                categories = uxErrorResponse.categories ?: emptyList()
            )
        )

    fun fromResponseBody(exception: HttpException): NabuApiException {
        return exception.response()?.errorBody()?.string()?.let { errorBody ->
            val errorResponse = try {
                json.decodeFromString<NabuErrorResponse>(errorBody)
            } catch (ex: Exception) {
                null
            }
            errorResponse?.let {
                val id = it.id
                val httpErrorCode = exception.code()
                val errorType = it.type
                val errorDescription = it.description
                val errorCode = it.code
                val path = exception.response()?.raw()?.request?.url?.pathSegments?.joinToString(" , ")
                val serverSideUxError = it.ux?.let { nabuUxResponse ->
                    ServerSideUxErrorInfo(
                        id = nabuUxResponse.id,
                        title = nabuUxResponse.title,
                        description = nabuUxResponse.message,
                        iconUrl = nabuUxResponse.icon?.url.orEmpty(),
                        statusUrl = nabuUxResponse.icon?.status?.url.orEmpty(),
                        actions = nabuUxResponse.mapActions(),
                        categories = nabuUxResponse.categories ?: emptyList()
                    )
                }

                NabuApiException(
                    "$httpErrorCode: $errorType - $errorDescription - $errorCode - $path",
                    httpErrorCode,
                    errorType,
                    errorCode,
                    errorDescription,
                    path,
                    id,
                    serverSideUxError
                )
            }
        } ?: NabuApiException.fromErrorMessageAndCode(exception.message(), exception.code())
    }
}

fun Throwable.isInternetConnectionError(): Boolean =
    this is SocketTimeoutException ||
        this is IOException
