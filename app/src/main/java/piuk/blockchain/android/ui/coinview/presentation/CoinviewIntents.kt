package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.walletmode.WalletMode
import com.blockchain.data.DataResource
import com.github.mikephil.charting.data.Entry

sealed interface CoinviewIntents : Intent<CoinviewModelState> {
    /**
     * Triggers loading:
     * * asset price / chart values
     * * asset accounts
     * * recurring buys
     * * todo
     */
    object LoadAllData : CoinviewIntents

    /**
     * Load asset price and chart data
     */
    object LoadPriceData : CoinviewIntents

    /**
     * Load total balance and accounts
     */
    object LoadAccountsData : CoinviewIntents

    /**
     * Load recurring buys / show upsell when no data (if eligible)
     * Not supported by [WalletMode.NON_CUSTODIAL_ONLY]
     */
    object LoadRecurringBuysData : CoinviewIntents {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.walletMode != WalletMode.NON_CUSTODIAL_ONLY
        }
    }

    /**
     * Performs price updates while chart is interactive
     */
    data class UpdatePriceForChartSelection(val entry: Entry) : CoinviewIntents {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return (modelState.assetPriceHistory as? DataResource.Data)?.data?.historicRates?.isNotEmpty() ?: false
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
            return (modelState.assetPriceHistory as? DataResource.Data)?.data?.priceDetail?.timeSpan != timeSpan
        }
    }

    object RecurringBuysUpsell : CoinviewIntents

    data class ShowRecurringBuyDetail(val recurringBuyId: String) : CoinviewIntents
}
