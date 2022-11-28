package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class EarnDashboardViewState(
    val dashboardState: DashboardState
) : ViewState

sealed class EarnDashboardError {
    object None : EarnDashboardError()
    object DataFetchFailed : EarnDashboardError()
}

sealed class DashboardState {
    object Loading : DashboardState()
    object ShowKyc : DashboardState()
    class ShowError(val error: EarnDashboardError) : DashboardState()
    class OnlyDiscover(val discover: List<EarnAsset>) : DashboardState()
    class EarningAndDiscover(val earning: List<EarnAsset>, val discover: List<EarnAsset>) : DashboardState()
}

data class EarnAsset(
    val asset: AssetInfo,
    val rate: Double,
    val eligibility: EarnEligibility,
    val balanceCrypto: Money,
    val balanceFiat: Money
)

sealed class EarnEligibility {
    object Eligible : EarnEligibility()
    class NotEligible(val reason: EarnIneligibleReason) : EarnEligibility()
}

enum class EarnIneligibleReason {
    REGION,
    KYC_TIER,
    OTHER
}
