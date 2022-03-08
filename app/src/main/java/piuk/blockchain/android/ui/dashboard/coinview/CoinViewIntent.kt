package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

sealed class CoinViewIntent : MviIntent<CoinViewState> {

    class LoadAccounts(val asset: CryptoAsset) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.LoadingWallets)
    }

    class LoadAsset(val assetTicker: String) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState = oldState
    }

    class LoadRecurringBuys(val asset: AssetInfo) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState = oldState.copy(
            viewState = CoinViewViewState.LoadingRecurringBuys
        )
    }

    class LoadQuickActions(
        val asset: AssetInfo,
        val totalCryptoBalance: Money,
        val actionableAccount: BlockchainAccount
    ) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState = oldState.copy(
            viewState = CoinViewViewState.LoadingQuickActions
        )
    }

    class AssetLoaded(private val asset: CryptoAsset, private val selectedFiat: FiatCurrency) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(asset = asset, selectedFiat = selectedFiat)
    }

    class LoadNewChartPeriod(val timePeriod: HistoricalTimeSpan) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.LoadingChart)
    }

    class LoadAssetChart(
        val asset: CryptoAsset,
        val assetPrice: Prices24HrWithDelta,
        val selectedFiat: FiatCurrency
    ) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(viewState = CoinViewViewState.LoadingChart)
    }

    class UpdateAccountDetails(
        val viewState: CoinViewViewState,
        val assetInformation: AssetInformation,
        val asset: CryptoAsset
    ) : CoinViewIntent() {
        override fun reduce(oldState: CoinViewState): CoinViewState =
            oldState.copy(
                viewState = viewState, assetPrices = assetInformation.prices
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
