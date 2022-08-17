package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.HistoricalTimeSpan
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    override fun reduce(state: CoinviewModelState): CoinviewViewState = state.run {
        CoinviewViewState(
            assetName = asset?.currency?.name ?: "",
            price = CoinviewPriceState.Loading
        )
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
        when (intent) {
            CoinviewIntents.LoadData -> {
                loadChartData(
                    asset = modelState.asset!!,
                    timeSpan = HistoricalTimeSpan.DAY
                )
            }
        }
    }

    private fun loadChartData(asset: CryptoAsset, timeSpan: HistoricalTimeSpan) {
        viewModelScope.launch {
            asset.historicRateSeries(timeSpan).collectLatest { dataResource ->
                when(dataResource)
            }
        }
    }
}
