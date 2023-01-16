package com.blockchain.home.presentation.allassets

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.presentation.SectionSize
import com.blockchain.presentation.pulltorefresh.PullToRefreshUtils

sealed interface AssetsIntent : Intent<AssetsModelState> {
    data class LoadAccounts(
        val sectionSize: SectionSize,
        val forceRefresh: Boolean = false
    ) : AssetsIntent

    data class LoadFundLocks(
        val forceRefresh: Boolean = false
    ) : AssetsIntent {
        override fun isValidFor(modelState: AssetsModelState): Boolean {
            return modelState.fundsLocks !is DataResource.Data
        }
    }

    object LoadFilters : AssetsIntent

    data class FilterSearch(val term: String) : AssetsIntent {
        override fun isValidFor(modelState: AssetsModelState): Boolean {
            return modelState.accounts is DataResource.Data
        }
    }

    data class UpdateFilters(val filters: List<AssetFilter>) : AssetsIntent

    object Refresh : AssetsIntent {
        override fun isValidFor(modelState: AssetsModelState): Boolean {
            return PullToRefreshUtils.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
