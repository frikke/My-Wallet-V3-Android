package com.blockchain.prices.prices

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface PricesIntents : Intent<PricesModelState> {
    data class LoadData(val forceRefresh: Boolean = false) : PricesIntents {
        override fun isValidFor(modelState: PricesModelState): Boolean {
            return forceRefresh || modelState.data !is DataResource.Data
        }
    }

    data class FilterSearch(val term: String) : PricesIntents {
        override fun isValidFor(modelState: PricesModelState): Boolean {
            return modelState.data is DataResource.Data
        }
    }

    data class Filter(val filter: PricesFilter) : PricesIntents {
        override fun isValidFor(modelState: PricesModelState): Boolean {
            return modelState.data is DataResource.Data
        }
    }
}
