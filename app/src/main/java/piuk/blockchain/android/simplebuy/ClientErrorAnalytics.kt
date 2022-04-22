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
        val nabuApiException: NabuApiException?,
        val error: String,
        val source: Source,
        val title: String,
        val action: String? = null,
    ) : ClientErrorAnalytics(
        event = AnalyticsNames.CLIENT_ERROR.eventName,
        params = mapOf(
            "error" to error,
            "source" to source.name,
            "title" to title,
            "action" to action,
            "network_endpoint" to nabuApiException?.getPath(),
            "network_error_code" to nabuApiException?.getErrorCode()?.code.toString(),
            "network_error_description" to nabuApiException?.getErrorDescription(),
            "network_error_id" to nabuApiException?.getId(),
            "network_error_type" to nabuApiException?.getErrorType()?.type,
        ).withoutNullValues()
    )

    companion object {
        enum class Source {
            CLIENT, NABU, UNKNOWN
        }

        const val ACTION_BUY = "BUY"
        const val ACTION_SELL = "SELL"
        const val ACTION_SWAP = "SWAP"
        const val OOPS_ERROR = "OOPS_ERROR"
        const val NABU_ERROR = "NABU_ERROR"
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    }
}
