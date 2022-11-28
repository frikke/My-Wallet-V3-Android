package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class EarnDashboardViewState(
    val isLoading: Boolean,
    val needsToCompleteKyc: Boolean,
    val earnDashboardError: EarnDashboardError
) : ViewState

sealed class EarnDashboardError {
    object None : EarnDashboardError()
}
