package piuk.blockchain.android.ui.home

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class WalletClientAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    object WalletActivityViewed : WalletClientAnalytics(
        event = AnalyticsNames.WALLET_ACTIVITY_VIEWED.eventName
    )

    object WalletBuySellViewed : WalletClientAnalytics(
        event = AnalyticsNames.WALLET_BUY_SELL_VIEWED.eventName
    )

    object WalletHomeViewed : WalletClientAnalytics(
        event = AnalyticsNames.WALLET_HOME_VIEWED.eventName
    )

    object WalletFABViewed : WalletClientAnalytics(
        event = AnalyticsNames.WALLET_FAB_VIEWED.eventName
    )

    object WalletPricesViewed : WalletClientAnalytics(
        event = AnalyticsNames.WALLET_PRICES_VIEWED.eventName
    )

    object WalletRewardsViewed : WalletClientAnalytics(
        event = AnalyticsNames.WALLET_REWARDS_VIEWED.eventName
    )
}
