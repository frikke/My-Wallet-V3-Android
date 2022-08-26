package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.walletmode.WalletMode
import com.github.mikephil.charting.data.Entry
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction

sealed interface CoinviewIntent : Intent<CoinviewModelState> {
    /**
     * Triggers loading:
     * * asset price / chart values
     * * asset accounts
     * * recurring buys
     * * todo
     */
    object LoadAllData : CoinviewIntent

    /**
     * Load asset price and chart data
     */
    object LoadPriceData : CoinviewIntent

    /**
     * Load total balance and accounts
     */
    object LoadAccountsData : CoinviewIntent

    /**
     * Load recurring buys / show upsell when no data (if eligible)
     * Not supported by [WalletMode.NON_CUSTODIAL_ONLY]
     */
    object LoadRecurringBuysData : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.walletMode != WalletMode.NON_CUSTODIAL_ONLY
        }
    }

    /**
     * Load quick actions to setup the center and bottom buttons
     *
     * Should only load when accounts are already loaded
     * todo(othman) remove this check once accounts are cached
     * Should only load when balances are already loaded
     * todo(othman) remove this check once accounts are cached
     */
    object LoadQuickActions : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.accounts != null && modelState.totalBalance != null
        }
    }

    /**
     * Load asset description / website
     */
    object LoadAssetInfo : CoinviewIntent

    /**
     * Performs price updates while chart is interactive
     */
    data class UpdatePriceForChartSelection(val entry: Entry) : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.assetPriceHistory?.historicRates?.isNotEmpty() ?: false
        }
    }

    /**
     * Reset price to original value after chart interaction
     */
    object ResetPriceSelection : CoinviewIntent

    /**
     * Load a new time span chart
     */
    data class NewTimeSpanSelected(val timeSpan: HistoricalTimeSpan) : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.assetPriceHistory?.priceDetail?.timeSpan != timeSpan
        }
    }

    data class AccountSelected(val account: CoinviewAccount) : CoinviewIntent

    object RecurringBuysUpsell : CoinviewIntent

    data class ShowRecurringBuyDetail(val recurringBuyId: String) : CoinviewIntent

    data class QuickActionSelected(val quickAction: CoinviewQuickAction) : CoinviewIntent
}
