package piuk.blockchain.android.ui.prices.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.ui.prices.PricesModelState

sealed interface PricesIntents : Intent<PricesModelState> {
    class LoadAssetsAvailable(val fiatCurrency: FiatCurrency) : PricesIntents {
        override fun isValidFor(modelState: PricesModelState): Boolean {
            return modelState.fiatCurrency != fiatCurrency
        }
    }

    data class Search(val query: String) : PricesIntents

    data class Filter(val filter: PricesFilter) : PricesIntents

    data class PricesItemClicked(
        val cryptoCurrency: AssetInfo,
    ) : PricesIntents
}
