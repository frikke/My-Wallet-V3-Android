package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.HistoricalRate

data class CoinviewModelState(
    val asset: CryptoAsset? = null,

    val isPriceDataLoading: Boolean = false,
    val historicalRates: List<HistoricalRate>? = null,
) : ModelState
