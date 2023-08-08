package com.blockchain.prices.prices

import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.signedValue
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.flatMap
import com.blockchain.data.map
import com.blockchain.prices.R
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo

data class PricesViewState(
    val walletMode: WalletMode?,
    val selectedFilter: PricesFilter,
    val availableFilters: List<PricesFilter>,
    private val data: DataResource<Map<PricesOutputGroup, List<PriceItemViewState>>>,
    val topMovers: DataResource<List<PriceItemViewState>>
) : ViewState {
    val mostPopularAndOtherAssets: DataResource<Map<PricesOutputGroup, List<PriceItemViewState>>>
        get() = data

    val allAssets: DataResource<List<PriceItemViewState>>
        get() = data.map {
            (it[PricesOutputGroup.MostPopular] ?: listOf()) + (it[PricesOutputGroup.Others] ?: listOf())
        }
}

data class PriceItemViewState(
    val asset: AssetInfo,
    val data: BalanceChange
)

@StringRes fun PricesFilter.nameRes() = when (this) {
    PricesFilter.All -> com.blockchain.stringResources.R.string.prices_filter_all
    PricesFilter.Favorites -> com.blockchain.stringResources.R.string.prices_filter_favorites
    PricesFilter.Tradable -> com.blockchain.stringResources.R.string.prices_filter_tradable
}

enum class PricesOutputGroup {
    MostPopular, Others
}

/**
 * (percentage,position) of [asset]
 */
fun DataResource<List<PriceItemViewState>>.percentAndPositionOf(
    asset: AssetInfo
): Pair<Double, Int>? {
    return flatMap { list ->
        val item = list.first { it.asset == asset }
        item.data.delta.map { it.signedValue to list.indexOf(item) + 1 }
    }.dataOrElse(null)
}
