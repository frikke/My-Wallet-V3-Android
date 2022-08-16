package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.charts.PercentageChangeData

data class CoinviewViewState(
    val assetName: String,
    val price: CoinviewPriceState
) : ViewState

// BALANCE
sealed interface CoinviewPriceState {
    object Loading : CoinviewPriceState
    data class Data(
        val assetName: String,
        val assetLogo: String,
        val priceFormattedWithFiatSymbol: String,
        val percentageChangeData: PercentageChangeData
    ) : CoinviewPriceState
}