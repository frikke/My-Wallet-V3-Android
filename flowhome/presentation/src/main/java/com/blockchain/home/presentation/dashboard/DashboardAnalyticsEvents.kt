package com.blockchain.home.presentation.dashboard

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.coincore.AssetAction
import com.blockchain.walletmode.WalletMode

sealed class DashboardAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    data class ModeViewed(val walletMode: WalletMode) : DashboardAnalyticsEvents(
        event = String.format(
            AnalyticsNames.SUPERAPP_MODE_VIEWED.eventName,
            walletMode.modeName()
        )
    )

    data class EmptyStateBuyBtc(val amount: String?) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_EMPTY_BUY_BTC_CLICKED.eventName,
        params = mapOf(BTC_BUY_QUICK_FILL_AMOUNT to (amount ?: BTC_BUY_OTHER_AMOUNT))
    )

    object EmptyStateBuyOther : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_EMPTY_BUY_OTHER_CLICKED.eventName
    )

    object EmptyStateReceiveCrypto : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_EMPTY_RECEIVE_CLICKED.eventName
    )

    data class QuickActionClicked(val action: AssetAction) : DashboardAnalyticsEvents(
        event = String.format(
            AnalyticsNames.SUPERAPP_QUICK_ACTION_CLICKED.eventName,
            action.actionName()
        )
    )

    data class AssetsSeeAllClicked(val assetsCount: Int) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_ASSETS_SEE_ALL_CLICKED.eventName,
        params = mapOf(ASSETS_COUNT to assetsCount.toString())
    )

    data class CryptoAssetClicked(val ticker: String) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_CRYPTO_ASSET_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class FiatAssetClicked(val ticker: String) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_FIAT_ASSET_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class FiatAddCashClicked(val ticker: String) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_FIAT_ADD_CASH_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class FiatCashOutClicked(val ticker: String) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_FIAT_CASH_OUT_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    object EarnGetStartedClicked : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_EARN_GET_STARTED_CLICKED.eventName
    )

    object EarnManageClicked : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_EARN_MANAGE_CLICKED.eventName
    )

    object ActivitySeeAllClicked : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_ACTIVITY_SEE_ALL_CLICKED.eventName
    )

    object SupportClicked : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_SUPPORT_CLICKED.eventName
    )

    companion object {
        private const val BTC_BUY_QUICK_FILL_AMOUNT = "quick_fill_amount"
        private const val BTC_BUY_OTHER_AMOUNT = "OTHER"
        private const val ASSETS_COUNT = "number_assets"
        private const val CURRENCY = "currency"
    }
}

private fun WalletMode.modeName() = when (this) {
    WalletMode.CUSTODIAL -> "BCDC Account"
    WalletMode.NON_CUSTODIAL -> "DeFi"
}

private fun AssetAction.actionName() = when (this) {
    AssetAction.Swap -> "Swap"
    AssetAction.Sell -> "Sell"
    AssetAction.Send -> "Send"
    AssetAction.FiatWithdraw -> "Cash Out"
    AssetAction.ViewActivity -> TODO()
    AssetAction.ViewStatement -> TODO()
    AssetAction.Buy -> TODO()
    AssetAction.FiatDeposit -> TODO()
    AssetAction.Receive -> TODO()
    AssetAction.InterestDeposit -> TODO()
    AssetAction.InterestWithdraw -> TODO()
    AssetAction.Sign -> TODO()
    AssetAction.StakingDeposit -> TODO()
}