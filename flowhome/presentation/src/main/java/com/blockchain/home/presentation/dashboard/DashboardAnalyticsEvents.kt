package com.blockchain.home.presentation.dashboard

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.coincore.AssetAction
import com.blockchain.home.presentation.dashboard.composable.DashboardState
import com.blockchain.home.presentation.earn.EarnType
import com.blockchain.walletmode.WalletMode

sealed class DashboardAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    data class ModeViewed(val walletMode: WalletMode) : DashboardAnalyticsEvents(
        event = when (walletMode) {
            WalletMode.CUSTODIAL -> AnalyticsNames.SUPERAPP_MODE_CUSTODIAL_VIEWED.eventName
            WalletMode.NON_CUSTODIAL -> AnalyticsNames.SUPERAPP_MODE_NON_CUSTODIAL_VIEWED.eventName
        }
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

    data class QuickActionClicked(
        val assetAction: AssetAction,
        val state: DashboardState?
    ) : DashboardAnalyticsEvents(
        event = assetAction.eventName() ?: error("not supported"),
        params = state?.stateName()?.let { mapOf(DASHBOARD_STATE to it) } ?: emptyMap()
    )

    data class TopMoverAssetClicked(
        val ticker: String,
        val percentageMove: Double,
        val position: Int,
    ) : DashboardAnalyticsEvents(
        event = AnalyticsNames.TOP_MOVER_DASHBOARD_CLICKED.eventName,
        params = mapOf(
            CURRENCY to ticker,
            PERCENTAGE_MOVE to percentageMove.toString(),
            POSITION to position.toString()
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

    data class EarnAssetClicked(
        val currency: String,
        val product: EarnType
    ) : DashboardAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_EARN_ASSET_CLICKED.eventName,
        params = mapOf(
            CURRENCY to currency,
            EARN_PRODUCT to product.name
        )
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
        private const val EARN_PRODUCT = "earn_product"
        private const val DASHBOARD_STATE = "dashboard_state"
        private const val PERCENTAGE_MOVE = "percentage move"
        private const val POSITION = "position"
    }
}

fun AssetAction.eventName() = when (this) {
    AssetAction.Buy -> AnalyticsNames.SUPERAPP_QUICK_ACTION_BUY_CLICKED.eventName
    AssetAction.Swap -> AnalyticsNames.SUPERAPP_QUICK_ACTION_SWAP_CLICKED.eventName
    AssetAction.Sell -> AnalyticsNames.SUPERAPP_QUICK_ACTION_SELL_CLICKED.eventName
    AssetAction.Receive -> AnalyticsNames.SUPERAPP_QUICK_ACTION_RECEIVE_CLICKED.eventName
    AssetAction.Send -> AnalyticsNames.SUPERAPP_QUICK_ACTION_SEND_CLICKED.eventName
    AssetAction.FiatDeposit -> AnalyticsNames.SUPERAPP_QUICK_ACTION_ADD_CASH_CLICKED.eventName
    AssetAction.FiatWithdraw -> AnalyticsNames.SUPERAPP_QUICK_ACTION_CASH_OUT_CLICKED.eventName
    AssetAction.ViewActivity,
    AssetAction.ViewStatement,
    AssetAction.InterestDeposit,
    AssetAction.InterestWithdraw,
    AssetAction.Sign,
    AssetAction.StakingDeposit,
    AssetAction.ActiveRewardsDeposit -> null
}

private fun DashboardState.stateName() = when (this) {
    DashboardState.EMPTY -> "EMPTY_STATE"
    DashboardState.NON_EMPTY -> "NON_EMPTY_STATE"
    DashboardState.UNKNOWN -> null
}
