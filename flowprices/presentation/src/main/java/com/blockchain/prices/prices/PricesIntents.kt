package com.blockchain.prices.prices

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo

sealed interface PricesIntents : Intent<PricesModelState> {
    object LoadAssetsAvailable : PricesIntents

    data class Search(val query: String) : PricesIntents

    data class Filter(val filter: PricesFilter) : PricesIntents

    data class PricesItemClicked(
        val cryptoCurrency: AssetInfo,
    ) : PricesIntents
}
