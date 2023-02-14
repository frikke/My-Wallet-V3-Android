package com.blockchain.prices.prices

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.prices.domain.AssetPriceInfo

data class PricesModelState(
    val filters: List<PricesFilter> = emptyList(),
    val data: DataResource<List<AssetPriceInfo>> = DataResource.Loading,
    val topMoversCount: Int = 4,
    val filterTerm: String = "",
    val filterBy: PricesFilter = PricesFilter.All,
    val lastFreshDataTime: Long = 0
) : ModelState

enum class PricesFilter {
    All, Tradable, Favorites
}
