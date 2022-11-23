package piuk.blockchain.android.ui.prices.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import info.blockchain.balance.AssetInfo

data class PricesViewState(
    val isLoading: Boolean,
    val isError: Boolean,
    val selectedFilter: PricesFilter,
    val availableFilters: List<PricesFilter>,
    val data: List<PriceItemViewState>,
) : ViewState

data class PriceItemViewState(
    val hasError: Boolean,
    val assetInfo: AssetInfo,
    val delta: Double?,
    val currentPrice: String
)
