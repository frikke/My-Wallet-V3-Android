package com.blockchain.home.presentation.allassets

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.model.AssetFilterStatus

sealed interface AssetsIntent : Intent<AssetsModelState> {
    data class LoadData(val sectionSize: SectionSize) : AssetsIntent

    data class FilterSearch(val term: String) : AssetsIntent {
        override fun isValidFor(modelState: AssetsModelState): Boolean {
            return modelState.accounts is DataResource.Data
        }
    }

    data class UpdateFilters(val filters: List<AssetFilterStatus>) : AssetsIntent
}
