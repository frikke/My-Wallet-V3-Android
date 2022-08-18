package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase

class CoinviewViewModel(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val getAssetPriceUseCase: GetAssetPriceUseCase
) : MviViewModel<
    CoinviewIntents,
    CoinviewViewState,
    CoinviewModelState,
    CoinviewNavigationEvent,
    CoinviewArgs>(CoinviewModelState()) {

    private var loadPriceDataJob: Job? = null

    override fun viewCreated(args: CoinviewArgs) {
        (coincore[args.networkTicker] as? CryptoAsset)?.let { asset ->
            updateState {
                it.copy(
                    asset = asset,
                    isPriceDataLoading = true
                )
            }
        } ?: error("")
    }

    override fun reduce(state: CoinviewModelState): CoinviewViewState = state.run {
        CoinviewViewState(
            fatalError = CoinviewFatalError.None,
            assetName = asset?.currency?.name ?: "",
            assetPrice = when {
                isPriceDataLoading -> {
                    CoinviewPriceState.Loading
                }

                isPriceDataError -> {
                    CoinviewPriceState.Error
                }

                assetPrice != null -> {
                    CoinviewPriceState.Data(
                        assetName = asset?.currency?.name ?: "",
                        assetLogo = asset?.currency?.logo ?: "",
                        priceFormattedWithFiatSymbol = assetPrice!!.price.toStringWithSymbol(),
                        priceChangeFormattedWithFiatSymbol = assetPrice.changeDifference.toStringWithSymbol(),
                        percentChange = assetPrice.percentChange,
                        intervalName = when (assetPrice.timeSpan) {
                            HistoricalTimeSpan.DAY -> R.string.coinview_price_day
                            HistoricalTimeSpan.WEEK -> R.string.coinview_price_week
                            HistoricalTimeSpan.MONTH -> R.string.coinview_price_month
                            HistoricalTimeSpan.YEAR -> R.string.coinview_price_year
                            HistoricalTimeSpan.ALL_TIME -> R.string.coinview_price_all
                            else -> R.string.empty
                        },
                        chartData = assetPrice.historicRates.map { point ->
                            ChartEntry(
                                point.timestamp.toFloat(),
                                point.rate.toFloat()
                            )
                        },
                        selectedTimeSpan = assetPrice.timeSpan
                    )
                }

                else -> {
                    CoinviewPriceState.Loading
                }
            }
        )
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
        when (intent) {
            is CoinviewIntents.LoadData -> {
                loadPriceData(
                    asset = modelState.asset!!,
                    currentTimeSpan = modelState.assetPrice?.timeSpan,
                    requestedTimeSpan = HistoricalTimeSpan.DAY
                )
            }
        }
    }

    private fun loadPriceData(
        asset: CryptoAsset,
        currentTimeSpan: HistoricalTimeSpan?,
        requestedTimeSpan: HistoricalTimeSpan
    ) {
        loadPriceDataJob?.cancel()

        loadPriceDataJob = viewModelScope.launch {
            getAssetPriceUseCase(
                asset, requestedTimeSpan, currencyPrefs.selectedFiatCurrency
            ).collectLatest { dataResource ->
                when (dataResource) {
                    is DataResource.Data -> {
                        if (dataResource.data.historicRates.isEmpty()) {
                            updateState {
                                it.copy(isPriceDataLoading = false, isPriceDataError = true)
                            }
                        } else {
                            updateState {
                                it.copy(isPriceDataLoading = false, isPriceDataError = true)
                            }

                            //                            updateState {
                            //                                it.copy(isPriceDataLoading = false, assetPrice = dataResource.data)
                            //                            }
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(isPriceDataLoading = false, isPriceDataError = true)
                        }
                    }

                    DataResource.Loading -> {
                        updateState {
                            // if loading a new timespan or no data is loaded yet
                            it.copy(isPriceDataLoading = currentTimeSpan != requestedTimeSpan || it.assetPrice == null)
                        }
                    }
                }
            }
        }
    }
}
