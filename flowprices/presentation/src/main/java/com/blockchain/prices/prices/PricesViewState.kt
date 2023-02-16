package com.blockchain.prices.prices

import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.data.map
import com.blockchain.prices.R
import info.blockchain.balance.AssetInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class PricesViewState(
    val selectedFilter: PricesFilter,
    val availableFilters: List<PricesFilter>,
    private val data: DataResource<Map<PricesOutputGroup, List<PriceItemViewState>>>,
    val topMovers: DataResource<List<PriceItemViewState>>
) : ViewState {
    val mostPopularAndOtherAssets: DataResource<Map<PricesOutputGroup, List<PriceItemViewState>>>
        get() = data
    val allAssets: DataResource<List<PriceItemViewState>>
        get() = combineDataResources(
            data.map { it[PricesOutputGroup.MostPopular] ?: listOf() },
            data.map { it[PricesOutputGroup.Others] ?: listOf() }
        ) { mostPopular, other ->
            (mostPopular + other)
        }
}

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

enum class PricesOutputGroup {
    MostPopular, Others
}