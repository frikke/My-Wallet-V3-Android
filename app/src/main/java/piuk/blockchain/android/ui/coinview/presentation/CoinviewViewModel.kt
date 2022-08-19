package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
import com.github.mikephil.charting.data.Entry
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice

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

    private val fiatCurrency: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    private val defaultTimeSpan = HistoricalTimeSpan.DAY

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
                isPriceDataLoading && assetPriceHistory == null -> {
                    // show loading when data is loading and no data is previously available
                    CoinviewPriceState.Loading
                }

                isPriceDataError -> {
                    CoinviewPriceState.Error
                }

                assetPriceHistory != null -> {
                    // priceFormattedWithFiatSymbol, priceChangeFormattedWithFiatSymbol, percentChange
                    // will contain values from interactiveAssetPrice to correspond with user interaction

                    // intervalName will be empty if user is interacting with the chart

                    CoinviewPriceState.Data(
                        assetName = asset?.currency?.name ?: "",
                        assetLogo = asset?.currency?.logo ?: "",
                        fiatSymbol = fiatCurrency.symbol,
                        priceFormattedWithFiatSymbol = (interactiveAssetPrice ?: assetPriceHistory.priceDetail)
                            .price.toStringWithSymbol(),
                        priceChangeFormattedWithFiatSymbol = (interactiveAssetPrice ?: assetPriceHistory.priceDetail)
                            .changeDifference.toStringWithSymbol(),
                        percentChange = (interactiveAssetPrice ?: assetPriceHistory.priceDetail).percentChange,
                        intervalName = if (interactiveAssetPrice != null) R.string.empty else
                            when ((assetPriceHistory.priceDetail).timeSpan) {
                                HistoricalTimeSpan.DAY -> R.string.coinview_price_day
                                HistoricalTimeSpan.WEEK -> R.string.coinview_price_week
                                HistoricalTimeSpan.MONTH -> R.string.coinview_price_month
                                HistoricalTimeSpan.YEAR -> R.string.coinview_price_year
                                HistoricalTimeSpan.ALL_TIME -> R.string.coinview_price_all
                            },
                        chartData = when {
                            isPriceDataLoading &&
                                requestedTimeSpan != null &&
                                assetPriceHistory.priceDetail.timeSpan != requestedTimeSpan -> {
                                // show chart loading when data is loading and a new timespan is selected
                                CoinviewPriceState.Data.CoinviewChart.Loading
                            }
                            else -> CoinviewPriceState.Data.CoinviewChart.Data(
                                assetPriceHistory.historicRates.map { point ->
                                    ChartEntry(
                                        point.timestamp.toFloat(),
                                        point.rate.toFloat()
                                    )
                                }
                            )
                        },
                        selectedTimeSpan = (interactiveAssetPrice ?: assetPriceHistory.priceDetail).timeSpan
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
                require(modelState.asset != null) { "asset not initialized" }
                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = modelState.assetPriceHistory?.priceDetail?.timeSpan ?: defaultTimeSpan
                )
            }

            is CoinviewIntents.UpdatePriceForChartSelection -> {
                updatePriceForChartSelection(intent.entry, modelState.assetPriceHistory?.historicRates!!)
            }

            is CoinviewIntents.ResetPriceSelection -> {
                resetPriceSelection()
            }

            is CoinviewIntents.NewTimeSpanSelected -> {
                updateState { it.copy(requestedTimeSpan = intent.timeSpan) }

                loadPriceData(
                    asset = modelState.asset!!,
                    requestedTimeSpan = intent.timeSpan,
                )
            }
        }
    }

    private fun loadPriceData(
        asset: CryptoAsset,
        requestedTimeSpan: HistoricalTimeSpan
    ) {
        loadPriceDataJob?.cancel()

        loadPriceDataJob = viewModelScope.launch {
            getAssetPriceUseCase(
                asset = asset, timeSpan = requestedTimeSpan, fiatCurrency = fiatCurrency
            ).collectLatest { dataResource ->
                when (dataResource) {
                    is DataResource.Data -> {
                        if (dataResource.data.historicRates.isEmpty()) {
                            updateState {
                                it.copy(
                                    isPriceDataLoading = false,
                                    isPriceDataError = true
                                )
                            }
                        } else {
                            updateState {
                                it.copy(
                                    isPriceDataLoading = false,
                                    assetPriceHistory = dataResource.data,
                                    requestedTimeSpan = null
                                )
                            }
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(
                                isPriceDataLoading = false,
                                isPriceDataError = true,
                            )
                        }
                    }

                    DataResource.Loading -> {
                        updateState {
                            it.copy(isPriceDataLoading = true)
                        }
                    }
                }
            }
        }
    }

    /**
     * Build a [CoinviewModelState.interactiveAssetPrice] based on the [entry] selected
     * to update the price information with
     */
    private fun updatePriceForChartSelection(
        entry: Entry,
        historicRates: List<HistoricalRate>,
    ) {
        historicRates.firstOrNull { it.timestamp.toFloat() == entry.x }?.let { selectedHistoricalRate ->
            val firstForPeriod = historicRates.first()
            val difference = selectedHistoricalRate.rate - firstForPeriod.rate

            val percentChange = (difference / firstForPeriod.rate)

            val changeDifference = Money.fromMajor(fiatCurrency, difference.toBigDecimal())

            updateState {
                it.copy(
                    interactiveAssetPrice = CoinviewAssetPrice(
                        price = Money.fromMajor(
                            fiatCurrency, selectedHistoricalRate.rate.toBigDecimal()
                        ),
                        timeSpan = it.assetPriceHistory!!.priceDetail.timeSpan,
                        changeDifference = changeDifference,
                        percentChange = percentChange
                    )
                )
            }
        }
    }

    /**
     * Reset [CoinviewModelState.interactiveAssetPrice] to update the price information with original value
     */
    private fun resetPriceSelection() {
        updateState { it.copy(interactiveAssetPrice = null) }
    }
}