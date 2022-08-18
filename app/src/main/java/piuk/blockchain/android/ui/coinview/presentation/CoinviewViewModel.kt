package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewIntent
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewViewState

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
            price = when (isPriceDataLoading) {
                true -> CoinviewPriceState.Loading
                false -> {
                    CoinviewPriceState.Data(
                        assetName = "JSZJ",
                        assetLogo = "jdzjdz",
                        priceFormattedWithFiatSymbol = "jdzjdz",
                        percentageChangeData = PercentageChangeData(
                            priceChange = "jdzjd",
                            percentChange = 0.2,
                            interval = "jdzjd"
                        ),
                        chartData = historicalRates?.map { point ->
                            ChartEntry(
                                point.timestamp.toFloat(),
                                point.rate.toFloat()
                            )
                        } ?: listOf(),
                    )
                }
            }
        )
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
        when (intent) {
            CoinviewIntents.LoadData -> {
                loadPriceData(
                    asset = modelState.asset!!,
                    timeSpan = HistoricalTimeSpan.DAY
                )
            }
        }
    }

    private fun loadPriceData(asset: CryptoAsset, timeSpan: HistoricalTimeSpan) {
        viewModelScope.launch {
            asset.historicRateSeries(timeSpan).collectLatest { dataResource: DataResource<HistoricalRateList> ->

                when (dataResource) {
                    is DataResource.Data -> {
                        if (dataResource.data.isEmpty()) {
                            //                            process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                        } else {
                            updateState {
                                it.copy(
                                    isPriceDataLoading = false,
                                    historicalRates = dataResource.data
                                )
                            }
//                            process(
//                                CoinViewIntent.UpdateViewState(
//                                    CoinViewViewState.ShowAssetInfo(
//                                        entries = dataResource.data.map { point ->
//                                            ChartEntry(
//                                                point.timestamp.toFloat(),
//                                                point.rate.toFloat()
//                                            )
//                                        },
//                                        prices = prices24Hr,
//                                        historicalRateList = dataResource.data,
//                                        selectedFiat = fiatCurrency
//                                    )
//                                )
//                            )
                        }
                    }

                    is DataResource.Error -> {
                        //                        process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                    }

                    DataResource.Loading -> {
                    }
                }
            }
        }
    }
}
