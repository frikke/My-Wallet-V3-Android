package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EarnDashboardIntent : Intent<EarnDashboardModelState> {
    class UpdateListFilter(val filter: EarnDashboardListFilter) : EarnDashboardIntent
    class UpdateSearchQuery(val searchTerm: String) : EarnDashboardIntent
    class ItemSelected(val earnAsset: EarnAsset) : EarnDashboardIntent
    object LoadEarn : EarnDashboardIntent
}
