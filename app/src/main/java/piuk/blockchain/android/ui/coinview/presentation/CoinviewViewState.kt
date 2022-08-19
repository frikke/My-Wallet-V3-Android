package piuk.blockchain.android.ui.coinview.presentation

import androidx.annotation.StringRes
import com.blockchain.charts.ChartEntry
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.price.HistoricalTimeSpan

data class CoinviewViewState(
    val assetName: String,
    val assetPrice: CoinviewPriceState,
    val totalBalance: CoinviewTotalBalance
) : ViewState

// Price
sealed interface CoinviewPriceState {
    object Loading : CoinviewPriceState
    object Error : CoinviewPriceState
    data class Data(
        val assetName: String,
        val assetLogo: String,
        val fiatSymbol: String,
        val price: String,
        val priceChange: String,
        val percentChange: Double,
        @StringRes val intervalName: Int,
        val chartData: CoinviewChart,
        val selectedTimeSpan: HistoricalTimeSpan
    ) : CoinviewPriceState {
        sealed interface CoinviewChart {
            object Loading : CoinviewChart
            data class Data(val chartData: List<ChartEntry>) : CoinviewChart
        }
    }
}

// Total balance
sealed interface CoinviewTotalBalance {
    object Loading : CoinviewTotalBalance
    data class Data(
        val assetName: String,
        val totalFiatBalance: String,
        val totalCryptoBalance: String,
    ) : CoinviewTotalBalance
}