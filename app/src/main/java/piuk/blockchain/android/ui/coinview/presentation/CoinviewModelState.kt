package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetInformation
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys

/**
 * @property assetPriceHistory - contains chart data + price and price change information
 * @property interactiveAssetPrice - price and price change information, used when user is interacting with the chart
 */
data class CoinviewModelState(
    val walletMode: WalletMode,

    val asset: CryptoAsset? = null,

    // price
    val isChartDataLoading: Boolean = false,
    val assetPriceHistory: DataResource<CoinviewAssetPriceHistory> = DataResource.Loading,
    val requestedTimeSpan: HistoricalTimeSpan? = null,
    val interactiveAssetPrice: CoinviewAssetPrice? = null,

    // asset info (accounts/non tradeable)
    val assetInfo: DataResource<CoinviewAssetInformation> = DataResource.Loading,

    // recurring buys
    val isRecurringBuysLoading: Boolean = false,
    val isRecurringBuysError: Boolean = false,
    val recurringBuys: CoinviewRecurringBuys? = null
    ) : ModelState
