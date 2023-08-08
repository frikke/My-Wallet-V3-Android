package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.home.presentation.SectionSize
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.walletmode.WalletMode
import java.util.concurrent.TimeUnit

sealed interface ActivityIntent<ACTIVITY_MODEL> : Intent<ActivityModelState<ACTIVITY_MODEL>> {
    data class LoadActivity<ACTIVITY_MODEL>(
        val sectionSize: SectionSize,
        val freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ) : ActivityIntent<ACTIVITY_MODEL>

    data class FilterSearch<ACTIVITY_MODEL>(val term: String) : ActivityIntent<ACTIVITY_MODEL> {
        override fun isValidFor(modelState: ActivityModelState<ACTIVITY_MODEL>): Boolean {
            return modelState.activityItems is DataResource.Data
        }
    }

    class Refresh<ACTIVITY_MODEL> : ActivityIntent<ACTIVITY_MODEL> {
        override fun isValidFor(modelState: ActivityModelState<ACTIVITY_MODEL>): Boolean {
            return modelState.walletMode == WalletMode.CUSTODIAL &&
                PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
