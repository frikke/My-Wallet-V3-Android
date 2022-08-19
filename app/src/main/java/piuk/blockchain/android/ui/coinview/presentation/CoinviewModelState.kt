package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory

data class CoinviewModelState(
    val asset: CryptoAsset? = null,

    val isPriceDataLoading: Boolean = false,
    val isPriceDataError: Boolean = false,
    val assetPriceHistory: CoinviewAssetPriceHistory? = null,
    val interactiveAssetPrice: CoinviewAssetPrice? = null

) : ModelState
