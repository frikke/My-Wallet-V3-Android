package piuk.blockchain.android.ui.home.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class BuyDefiAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object BuySelected : BuyDefiAnalyticsEvents(
        event = AnalyticsNames.MVP_DEFI_BUY_SELECTED.eventName
    )

    object SwitchedToTrading : BuyDefiAnalyticsEvents(
        event = AnalyticsNames.MVP_DEFI_BUY_SWITCH_TO_TRADING.eventName
    )
}
