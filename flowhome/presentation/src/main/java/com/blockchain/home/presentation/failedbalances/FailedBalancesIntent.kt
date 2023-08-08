package com.blockchain.home.presentation.failedbalances

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.presentation.pulltorefresh.PullToRefresh

sealed interface FailedBalancesIntent : Intent<FailedBalancesModelState> {
    object LoadData : FailedBalancesIntent

    object DismissFailedNetworksWarning : FailedBalancesIntent

    object Refresh : FailedBalancesIntent {
        override fun isValidFor(modelState: FailedBalancesModelState): Boolean {
            return PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
