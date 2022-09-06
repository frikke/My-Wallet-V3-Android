package piuk.blockchain.android.ui.dashboard.walletmode

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.UserAnalytics
import com.blockchain.analytics.UserProperty
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.walletmode.WalletMode

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

interface WalletModeReporter {
    fun reportMvpEnabled(isEnabled: Boolean)
    fun reportWalletMode(walletMode: WalletMode)
}

class WalletModeReporterImpl(
    private val userAnalytics: UserAnalytics
) : WalletModeReporter {
    override fun reportMvpEnabled(isEnabled: Boolean) {
        userAnalytics.logUserProperty(
            UserProperty(
                property = UserAnalytics.IS_SUPERAPP_MVP,
                value = isEnabled.toString()
            )
        )
    }

    override fun reportWalletMode(walletMode: WalletMode) {
        if (walletMode != WalletMode.UNIVERSAL) {
            userAnalytics.logUserProperty(
                UserProperty(
                    property = UserAnalytics.WALLET_MODE,
                    value = when (walletMode) {
                        WalletMode.NON_CUSTODIAL_ONLY -> "PKW"
                        WalletMode.CUSTODIAL_ONLY -> "TRADING"
                        else -> ""
                    }
                )
            )
        }
    }
}
