package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import info.blockchain.balance.AssetInfo

sealed class EarnDashboardNavigationEvent : NavigationEvent {
    class OpenInterestSummarySheet(val assetTicker: String) : EarnDashboardNavigationEvent()
    class OpenStakingSummarySheet(val assetTicker: String) : EarnDashboardNavigationEvent()
    class OpenActiveRewardsSummarySheet(val assetTicker: String) : EarnDashboardNavigationEvent()
    object OpenKycUpgradeNowSheet : EarnDashboardNavigationEvent()
    class OpenBlockedForRegionSheet(val earnType: EarnType) : EarnDashboardNavigationEvent()
    class OpenBuyOrReceiveSheet(val assetAction: AssetAction, val availableToBuy: Boolean, val account: SingleAccount) :
        EarnDashboardNavigationEvent()

    class OpenUrl(val url: String) : EarnDashboardNavigationEvent()
    class OpenReceive(val networkTicker: String) : EarnDashboardNavigationEvent()
    class OpenBuy(val assetInfo: AssetInfo) : EarnDashboardNavigationEvent()
    object OpenKyc : EarnDashboardNavigationEvent()
    class OpenProductComparator(val earnProducts: List<EarnType>) : EarnDashboardNavigationEvent()
}
