package piuk.blockchain.android.ui.coinview.presentation

import androidx.annotation.StringRes
import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.HistoricalTimeSpan

data class CoinviewViewState(
    val fatalError: CoinviewFatalError,
    val assetName: String,
    val assetPrice: CoinviewPriceState
) : ViewState

// FATAL ERROR
sealed interface CoinviewFatalError {
    object None : CoinviewFatalError
    object Price : CoinviewFatalError
}

// BALANCE
sealed interface CoinviewPriceState {
    object Loading : CoinviewPriceState
    object Error : CoinviewPriceState
    data class Data(
        val assetName: String,
        val assetLogo: String,
        val priceFormattedWithFiatSymbol: String,
        val priceChangeFormattedWithFiatSymbol: String,
        val percentChange: Double,
        @StringRes val intervalName: Int,
        val chartData: List<ChartEntry>,
        val selectedTimeSpan: HistoricalTimeSpan
    ) : CoinviewPriceState
}