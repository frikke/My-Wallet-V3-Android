package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class CoinviewViewModel(
    private val coincore: Coincore,
) : MviViewModel<
    CoinviewIntents,
    CoinviewViewState,
    CoinviewModelState,
    CoinviewNavigationEvent,
    CoinviewArgs>(CoinviewModelState()) {

    override fun viewCreated(args: CoinviewArgs) {
        (coincore[args.networkTicker] as? CryptoAsset)?.let { asset ->
            updateState { it.copy(asset = asset) }
        } ?: error("")
    }

    override fun reduce(state: CoinviewModelState): CoinviewViewState {
        return CoinviewViewState(
            networkTicker = state.asset?.currency?.networkTicker ?: ""
        )
    }

    private fun loadData() {
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
    }
}
