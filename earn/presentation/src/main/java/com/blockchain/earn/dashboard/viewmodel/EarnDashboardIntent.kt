package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EarnDashboardIntent : Intent<EarnDashboardModelState> {
    object LoadEarn : EarnDashboardIntent
}
