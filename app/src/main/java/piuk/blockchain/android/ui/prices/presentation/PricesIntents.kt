package piuk.blockchain.android.ui.prices.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.prices.PricesModelState

sealed interface PricesIntents : Intent<PricesModelState> {
    object LoadAssetsAvailable : PricesIntents

    data class LoadPrice(
        val cryptoCurrency: AssetInfo,
    ) : PricesIntents

    data class FilterData(val filter: String) : PricesIntents

    data class PricesItemClicked(
        val cryptoCurrency: AssetInfo,
    ) : PricesIntents
}
