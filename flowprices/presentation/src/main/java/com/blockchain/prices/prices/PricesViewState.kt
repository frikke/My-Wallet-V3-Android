package com.blockchain.prices.prices

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo

data class PricesViewState(
    val selectedFilter: PricesFilter,
    val availableFilters: List<PricesFilter>,
    val data: DataResource<List<PriceItemViewState>>,
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
