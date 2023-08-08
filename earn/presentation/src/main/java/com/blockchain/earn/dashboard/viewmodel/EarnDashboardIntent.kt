package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EarnDashboardIntent : Intent<EarnDashboardModelState> {
    class UpdateEarningTabListFilter(val filter: EarnDashboardListFilter) : EarnDashboardIntent
    class UpdateEarningTabSearchQuery(val searchTerm: String) : EarnDashboardIntent
    class UpdateDiscoverTabListFilter(val filter: EarnDashboardListFilter) : EarnDashboardIntent
    class UpdateDiscoverTabSearchQuery(val searchTerm: String) : EarnDashboardIntent
    class EarningItemSelected(val earnAsset: EarnAsset) : EarnDashboardIntent
    class DiscoverItemSelected(val earnAsset: EarnAsset) : EarnDashboardIntent
    object LaunchProductComparator : EarnDashboardIntent
    object StartKycClicked : EarnDashboardIntent
    object LoadEarn : EarnDashboardIntent
    object LoadSilently : EarnDashboardIntent
    object FinishOnboarding : EarnDashboardIntent
}
