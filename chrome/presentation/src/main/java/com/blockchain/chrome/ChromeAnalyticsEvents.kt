package com.blockchain.chrome

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.walletmode.WalletMode

sealed class ChromeAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    data class ModeClicked(
        val walletMode: WalletMode
    ) : ChromeAnalyticsEvents(
        event = when (walletMode) {
            WalletMode.CUSTODIAL -> AnalyticsNames.SUPERAPP_MODE_CUSTODIAL_CLICKED.eventName
            WalletMode.NON_CUSTODIAL -> AnalyticsNames.SUPERAPP_MODE_NON_CUSTODIAL_CLICKED.eventName
        }
    )

    data class ModeLongClicked(
        val walletMode: WalletMode
    ) : ChromeAnalyticsEvents(
        event = when (walletMode) {
            WalletMode.CUSTODIAL -> AnalyticsNames.SUPERAPP_MODE_CUSTODIAL_LONG_CLICK.eventName
            WalletMode.NON_CUSTODIAL -> AnalyticsNames.SUPERAPP_MODE_NON_CUSTODIAL_LONG_CLICK.eventName
        }
    )
}
