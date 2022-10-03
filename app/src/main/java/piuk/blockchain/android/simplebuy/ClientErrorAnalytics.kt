package piuk.blockchain.android.simplebuy

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.api.NabuApiException
import com.blockchain.extensions.withoutNullValues
import java.io.Serializable

sealed class ClientErrorAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap(),
) : AnalyticsEvent {

    class ClientLogError(
        val errorId: String? = null,
        val nabuApiException: NabuApiException?,
        val errorDescription: String? = null,
        val error: String,
        val source: Source,
        val title: String,
        val action: String? = null,
        val categories: List<String>
    ) : ClientErrorAnalytics(
        event = AnalyticsNames.CLIENT_ERROR.eventName,
        params = mapOf(
            "id" to (errorId ?: nabuApiException?.getServerSideErrorInfo()?.id.orEmpty()),
            "error" to error,
            "source" to source.name,
            "title" to title,
            "action" to action,
            "network_endpoint" to nabuApiException?.getPath(),
            "network_error_code" to nabuApiException?.getErrorCode()?.code?.or(-1),
            "network_error_description" to (nabuApiException?.message ?: errorDescription.orEmpty()),
            "network_error_id" to nabuApiException?.getId(),
            "network_error_type" to nabuApiException?.getErrorType()?.type,
            "categories" to categories.joinToString(",")
        ).withoutNullValues()
    )

    companion object {
        enum class Source {
            CLIENT, NABU, UNKNOWN
        }

        const val ACTION_BUY = "BUY"
        const val ACTION_SELL = "SELL"
        const val ACTION_SWAP = "SWAP"
        const val ACTION_UNKNOWN = "UNKNOWN"

        const val OOPS_ERROR = "OOPS_ERROR"
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
        const val NABU_ERROR = "NABU_ERROR"
        const val INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"
        const val BELOW_FEES = "BELOW_FEES"
        const val BELOW_MINIMUM_LIMIT = "BELOW_MINIMUM_LIMIT"
        const val OVER_MAXIMUM_SOURCE_LIMIT = "OVER_MAXIMUM_SOURCE_LIMIT"
        const val OVER_MAXIMUM_PERSONAL_LIMIT = "OVER_MAXIMUM_PERSONAL_LIMIT"
        const val ADDRESS_IS_CONTRACT = "ADDRESS_IS_CONTRACT"
        const val INVALID_ADDRESS = "INVALID_ADDRESS"
        const val INVALID_PASSWORD = "INVALID_PASSWORD"
        const val OPTION_INVALID = "OPTION_INVALID"
        const val PENDING_ORDERS_LIMIT_REACHED = "PENDING_ORDERS_LIMIT_REACHED"
        const val TRANSACTION_IN_FLIGHT = "TRANSACTION_IN_FLIGHT"
        const val FATAL_ERROR = "FATAL_ERROR"
        const val ORDER_NOT_CANCELABLE = "ORDER_NOT_CANCELABLE"
        const val WITHDRAW_ALREADY_PENDING = "WITHDRAW_ALREADY_PENDING"
        const val WITHDRAW_BALANCE_LOCKED = "WITHDRAW_BALANCE_LOCKED"
        const val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
        const val INTERNET_CONNECTION_ERROR = "INTERNET_CONNECTION_ERROR"
        const val EXECUTION_FAILED = "EXECUTION_FAILED"
        const val INVALID_POSTCODE = "INVALID_POSTCODE"
        const val INVALID_AMOUNT = "INVALID_AMOUNT"
        const val INELIGIBLE = "INELIGIBLE"
        const val INVALID_QUOTE = "INVALID_QUOTE"
        const val ORDER_DIRECTION_DISABLED = "ORDER_DIRECTION_DISABLED"
        const val INVALID_FIAT_CURRENCY = "INVALID_FIAT_CURRENCY"
        const val INVALID_CRYPTO_CURRENCY = "INVALID_CRYPTO_CURRENCY"
        const val TRADING_DISABLED = "TRADING_DISABLED"
        const val SERVER_SIDE_HANDLED_ERROR = "SERVER_SIDE_HANDLED_ERROR"
        const val SETTLEMENT_REFRESH_REQUIRED = "SETTLEMENT_REFRESH_REQUIRED"
        const val SETTLEMENT_INSUFFICIENT_BALANCE = "SETTLEMENT_INSUFFICIENT_BALANCE"
        const val SETTLEMENT_STALE_BALANCE = "SETTLEMENT_STALE_BALANCE"
        const val SETTLEMENT_GENERIC_ERROR = "SETTLEMENT_GENERIC_ERROR"
    }
}
