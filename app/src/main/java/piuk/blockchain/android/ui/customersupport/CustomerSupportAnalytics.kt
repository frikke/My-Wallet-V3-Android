package piuk.blockchain.android.ui.customersupport

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

sealed class CustomerSupportAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    object SheetShown : CustomerSupportAnalytics(
        event = AnalyticsNames.CUSTOMER_SUPPORT_SHEET_SHOWN.eventName
    )

    object EmailClicked : CustomerSupportAnalytics(
        event = AnalyticsNames.CUSTOMER_SUPPORT_EMAIL_CLICKED.eventName
    )

    object FaqClicked : CustomerSupportAnalytics(
        event = AnalyticsNames.CUSTOMER_SUPPORT_FAQ_CLICKED.eventName
    )
}
