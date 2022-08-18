package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import piuk.blockchain.android.ui.coinview.domain.model.CoinViewAssetPrice

data class CoinviewModelState(
    val asset: CryptoAsset? = null,

    val isPriceDataLoading: Boolean = false,
    val assetPrice: CoinViewAssetPrice? = null,
) : ModelState
