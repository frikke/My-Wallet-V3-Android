package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class CoinviewViewState(
    val networkTicker: String
) : ViewState