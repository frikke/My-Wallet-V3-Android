package com.blockchain.chrome

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.walletmode.WalletMode

sealed class ChromeAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    data class ModeClicked(
        val walletMode: WalletMode
    ) : ChromeAnalyticsEvents(
        event = String.format(
            AnalyticsNames.SUPERAPP_MODE_CLICKED.eventName,
            walletMode.modeName()
        )
    )

    data class ModeLongClicked(
        val walletMode: WalletMode
    ) : ChromeAnalyticsEvents(
        event = String.format(
            AnalyticsNames.SUPERAPP_MODE_LONG_CLICK.eventName,
            walletMode.modeName()
        )
    )
}

private fun WalletMode.modeName() = when (this) {
    WalletMode.CUSTODIAL -> "BCDC Account"
    WalletMode.NON_CUSTODIAL -> "DeFi"
}
