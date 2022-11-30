package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
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
    data class ShowError(val error: EarnDashboardError) : DashboardState()
    data class OnlyDiscover(val discover: List<EarnAsset>) : DashboardState()
    data class EarningAndDiscover(val earning: List<EarnAsset>, val discover: List<EarnAsset>) : DashboardState()
}

data class EarnAsset(
    val assetTicker: String,
    val assetName: String,
    val iconUrl: String,
    val rate: Double,
    val eligibility: EarnEligibility,
    val balanceCrypto: Money,
    val balanceFiat: Money,
    val type: EarnType
)

sealed class EarnType {
    object Staking : EarnType()
    object Rewards : EarnType()
}

sealed class EarnEligibility {
    object Eligible : EarnEligibility()
    data class NotEligible(val reason: EarnIneligibleReason) : EarnEligibility()
}

enum class EarnIneligibleReason {
    REGION,
    KYC_TIER,
    OTHER
}

enum class EarnDashboardListFilter {
    All,
    Staking,
    Rewards
}
