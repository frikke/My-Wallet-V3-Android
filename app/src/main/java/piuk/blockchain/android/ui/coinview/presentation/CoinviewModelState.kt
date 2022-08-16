package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class CoinviewModelState(
    val asset: CryptoAsset? = null,
    val isLoading: Boolean = false
) : ModelState
