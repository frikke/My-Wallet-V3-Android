package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.core.price.HistoricalTimeSpan

sealed class CoinViewIntent : MviIntent<CoinViewState> {

    class LoadAssetInformation(val asset: CryptoAsset) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.LoadingWallets)
    }

    class LoadAsset(val assetTicker: String) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState = oldState
    }

    class AssetLoaded(private val asset: CryptoAsset) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(asset = asset)
    }

    class LoadNewChartPeriod(val timePeriod: HistoricalTimeSpan) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.LoadingChart)
    }

    class LoadAssetChart(
        val asset: CryptoAsset
    ) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.LoadingChart)
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
