package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.core.price.HistoricalTimeSpan
import com.github.mikephil.charting.data.Entry

sealed interface CoinviewIntents : Intent<CoinviewModelState> {
    /**
     * Triggers loading:
     * * asset price / chart values
     * * todo
     */
    object LoadData : CoinviewIntents

    /**
     * Performs price updates while chart is interactive
     */
    data class UpdatePriceForChartSelection(val entry: Entry) : CoinviewIntents {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.assetPriceHistory?.historicRates?.isNotEmpty() ?: false
        }
    }

    /**
     * Reset price to original value after chart interaction
     */
    object ResetPriceSelection : CoinviewIntents

    /**
     * Load a new time span chart
     */
    data class NewTimeSpanSelected(val timeSpan: HistoricalTimeSpan) : CoinviewIntents {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.assetPriceHistory?.priceDetail?.timeSpan != timeSpan
        }
    }
}
