package com.blockchain.prices.prices

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo

sealed interface PricesIntents : Intent<PricesModelState> {
    object LoadAssetsAvailable : PricesIntents

    data class FilterSearch(val term: String) : PricesIntents {
        override fun isValidFor(modelState: PricesModelState): Boolean {
            return modelState.data is DataResource.Data
        }
    }

    data class Filter(val filter: PricesFilter) : PricesIntents

    data class PricesItemClicked(
        val cryptoCurrency: AssetInfo,
    ) : PricesIntents
}
