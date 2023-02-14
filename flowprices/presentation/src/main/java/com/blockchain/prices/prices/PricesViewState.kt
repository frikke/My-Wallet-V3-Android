package com.blockchain.prices.prices

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.prices.R
import info.blockchain.balance.AssetInfo

data class PricesViewState(
    val selectedFilter: PricesFilter,
    val availableFilters: List<PricesFilter>,
    val data: DataResource<List<PriceItemViewState>>,
    val topMovers: DataResource<List<PriceItemViewState>>
) : ViewState

data class PriceItemViewState(
    val asset: AssetInfo,
    val name: String,
    val ticker: String,
    val network: String?,
    val logo: String,
    val delta: DataResource<ValueChange>,
    val currentPrice: DataResource<String>
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PriceItemViewState

        if (name != other.name) return false
        if (ticker != other.ticker) return false
        if (network != other.network) return false
        if (logo != other.logo) return false
        if (delta != other.delta) return false
        if (currentPrice != other.currentPrice) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ticker.hashCode()
        result = 31 * result + (network?.hashCode() ?: 0)
        result = 31 * result + logo.hashCode()
        result = 31 * result + delta.hashCode()
        result = 31 * result + currentPrice.hashCode()
        return result
    }
}

@StringRes fun PricesFilter.nameRes() = when (this) {
    PricesFilter.All -> R.string.prices_filter_all
    PricesFilter.Favorites -> R.string.prices_filter_favorites
    PricesFilter.Tradable -> R.string.prices_filter_tradable
}
