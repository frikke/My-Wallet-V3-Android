package piuk.blockchain.android.ui.prices.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import piuk.blockchain.android.ui.prices.PricesItem

data class PricesViewState(
    val isLoading: Boolean,
    val isError: Boolean,
    val data: List<PricesItem>,
) : ViewState
