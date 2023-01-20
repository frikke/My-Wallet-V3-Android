package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo

sealed interface EarnDashboardIntent : Intent<EarnDashboardModelState> {
    class UpdateEarningTabListFilter(val filter: EarnDashboardListFilter) : EarnDashboardIntent
    class UpdateEarningTabSearchQuery(val searchTerm: String) : EarnDashboardIntent
    class UpdateDiscoverTabListFilter(val filter: EarnDashboardListFilter) : EarnDashboardIntent
    class UpdateDiscoverTabSearchQuery(val searchTerm: String) : EarnDashboardIntent
    class EarningItemSelected(val earnAsset: EarnAsset) : EarnDashboardIntent
    class DiscoverItemSelected(val earnAsset: EarnAsset) : EarnDashboardIntent
    class CarouselLearnMoreSelected(val url: String) : EarnDashboardIntent
    object StartKycClicked : EarnDashboardIntent
    class OnNavigateToAction(val action: AssetAction, val assetInfo: AssetInfo) : EarnDashboardIntent
    object LoadEarn : EarnDashboardIntent
    object LoadSilently : EarnDashboardIntent
}
