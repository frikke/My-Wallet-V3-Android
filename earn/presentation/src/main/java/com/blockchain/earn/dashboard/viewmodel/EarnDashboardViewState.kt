package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import info.blockchain.balance.Money

data class EarnDashboardViewState(
    val dashboardState: DashboardState,
    val earningTabQueryBy: String,
    val earningTabFilterBy: EarnDashboardListFilter,
    val discoverTabQueryBy: String,
    val discoverTabFilterBy: EarnDashboardListFilter
) : ViewState

sealed class EarnDashboardError {
    object None : EarnDashboardError()
    object DataFetchFailed : EarnDashboardError()
}

sealed class DashboardState {
    object Loading : DashboardState()
    object ShowKyc : DashboardState()
    data class ShowIntro(val earnProductsAvailable: List<EarnType>) : DashboardState()
    data class ShowError(val error: EarnDashboardError) : DashboardState()
    data class OnlyDiscover(
        val discover: List<EarnAsset>,
        val filterList: List<EarnDashboardListFilter>
    ) : DashboardState()
    data class EarningAndDiscover(
        val earning: List<EarnAsset>,
        val totalEarningBalance: String,
        val totalEarningBalanceSymbol: String,
        val discover: List<EarnAsset>,
        val filterList: List<EarnDashboardListFilter>
    ) : DashboardState()
}

data class EarnAsset(
    val assetTicker: String,
    val assetName: String,
    val iconUrl: String,
    val rate: Double,
    val eligibility: EarnRewardsEligibility,
    val balanceCrypto: Money,
    val balanceFiat: Money?,
    val type: EarnType
)

sealed class EarnType {
    object Staking : EarnType()
    object Passive : EarnType()
    object Active : EarnType()
}

enum class EarnDashboardListFilter {
    All,
    Interest,
    Staking,
    Active
}
