package com.blockchain.home.presentation.allassets

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.presentation.SectionSize

sealed interface AssetsIntent : Intent<AssetsModelState> {
    data class LoadAccounts(val sectionSize: SectionSize) : AssetsIntent

    object LoadFundLocks : AssetsIntent {
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
}
