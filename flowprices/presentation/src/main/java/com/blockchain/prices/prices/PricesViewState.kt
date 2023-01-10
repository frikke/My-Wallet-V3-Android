package com.blockchain.prices.prices

import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.prices.R
import info.blockchain.balance.AssetInfo

data class PricesViewState(
    val selectedFilter: PricesFilter,
    val availableFilters: List<PricesFilter>,
    val data: DataResource<List<PriceItemViewState>>
) : ViewState

data class PriceItemViewState(
    val asset: AssetInfo,
    val name: String,
    val ticker: String,
    val network: String?,
    val logo: String,
    val delta: DataResource<ValueChange>,
    val currentPrice: DataResource<String>
)

@StringRes fun PricesFilter.nameRes() = when (this) {
    PricesFilter.All -> R.string.prices_filter_all
    PricesFilter.Favorites -> R.string.prices_filter_favorites
    PricesFilter.Tradable -> R.string.prices_filter_tradable
}
