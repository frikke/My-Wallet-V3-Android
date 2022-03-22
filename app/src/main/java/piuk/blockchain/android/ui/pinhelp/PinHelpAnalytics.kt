package piuk.blockchain.android.ui.pinhelp

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

sealed class PinHelpAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    object SheetShown : PinHelpAnalytics(
        event = AnalyticsNames.LOGIN_HELP_SHEET_SHOWN.eventName
    )

    object EmailClicked : PinHelpAnalytics(
        event = AnalyticsNames.LOGIN_HELP_EMAIL_CLICKED.eventName
    )

    object FaqClicked : PinHelpAnalytics(
        event = AnalyticsNames.LOGIN_HELP_FAQ_CLICKED.eventName
    )
}
