package com.blockchain.earn.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardNavigationEvent
import com.blockchain.earn.dashboard.viewmodel.EarnType

interface EarnNavigation {
    fun route(navigationEvent: EarnDashboardNavigationEvent)

    fun openInterestSummarySheet(assetTicker: String)

    fun openStakingSummarySheet(assetTicker: String)

    fun openActiveRewardsSummarySheet(assetTicker: String)

    fun showBlockedAccessSheet(title: String, paragraph: String)

    fun openExternalUrl(url: String)

    fun showBuyUpsellSheet(account: BlockchainAccount, action: AssetAction, canBuy: Boolean)

    fun startKycClicked()

    fun openProductComparatorBottomSheet(earnProducts: Map<EarnType, Double>)
}
