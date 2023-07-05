package com.blockchain.home.presentation.failedbalances

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.allassets.AssetsModelState
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.walletmode.WalletMode

sealed interface FailedBalancesIntent : Intent<FailedBalancesModelState> {
    object LoadData : FailedBalancesIntent

    object DismissFailedNetworksWarning : FailedBalancesIntent

    object Refresh : FailedBalancesIntent {
        override fun isValidFor(modelState: FailedBalancesModelState): Boolean {
            return PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
