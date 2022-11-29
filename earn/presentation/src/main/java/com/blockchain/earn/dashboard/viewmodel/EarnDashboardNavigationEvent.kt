package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed class EarnDashboardNavigationEvent : NavigationEvent {
    class OpenRewardsSummarySheet(val account: CryptoAccount) : EarnDashboardNavigationEvent()
    class OpenStakingSummarySheet(val assetTicker: String) : EarnDashboardNavigationEvent()
}
