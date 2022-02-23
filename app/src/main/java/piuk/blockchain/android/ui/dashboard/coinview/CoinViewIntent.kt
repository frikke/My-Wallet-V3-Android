package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class CoinViewIntent : MviIntent<CoinViewState> {

    class LoadAssetInformation(val assetTicker: String) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.Loading)
    }

    class AssetInfoLoaded(
        private val asset: CryptoAsset,
        private val assetDisplayMap: List<AssetDisplayInfo>
    ) :
        CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(
                asset = asset,
                viewState = CoinViewViewState.ShowAccountInfo(assetDisplayMap)
            )
    }

    class UpdateViewState(private val viewState: CoinViewViewState) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = viewState)
    }

    object ResetViewState : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.None)
    }

    class UpdateErrorState(private val errorState: CoinViewError) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(error = errorState)
    }

    object ResetErrorState : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(error = CoinViewError.None)
    }
}
