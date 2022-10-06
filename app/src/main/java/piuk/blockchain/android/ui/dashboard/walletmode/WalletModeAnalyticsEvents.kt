package piuk.blockchain.android.ui.dashboard.walletmode

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class WalletModeAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object SwitchedToDefi : WalletModeAnalyticsEvents(
        event = AnalyticsNames.MVP_SWITCHED_TO_DEFI.eventName
    )

    object SwitchedToTrading : WalletModeAnalyticsEvents(
        event = AnalyticsNames.MVP_SWITCHED_TO_TRADING.eventName
    )
}
