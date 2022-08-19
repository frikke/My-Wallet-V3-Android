package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.charts.ChartEntry
import com.blockchain.charts.ChartView
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.charts.Balance
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.control.TabLayoutLive
import com.blockchain.componentlib.system.LoadingChart
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.core.price.HistoricalTimeSpan
import com.github.mikephil.charting.data.Entry
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState
import kotlin.random.Random

@Composable
fun AssetPrice(
    data: CoinviewPriceState,
    onChartEntryHighlighted: (Entry) -> Unit,
    resetPriceInformation: () -> Unit
) {
    when (data) {
        CoinviewPriceState.Loading -> {
            AssetPriceInfoLoading()
        }

        CoinviewPriceState.Error -> {
            AssetPriceError()
        }

        is CoinviewPriceState.Data -> {
            AssetPriceInfoData(
                data = data,
                onChartEntryHighlighted = onChartEntryHighlighted,
                resetPriceInformation = resetPriceInformation
            )
        }
    }
}

@Composable
fun AssetPriceInfoLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerLoadingTableRow(showIconLoader = false)

        LoadingChart(
            historicalRates = List(20) {
                object : SparkLineHistoricalRate {
                    override val timestamp: Long = it.toLong()
                    override val rate: Double = Random.nextDouble(50.0, 150.0)
                }
            },
            loadingText = stringResource(R.string.coinview_chart_loading)
        )
    }
}

@Composable
fun AssetPriceInfoData(
    data: CoinviewPriceState.Data,
    onChartEntryHighlighted: (Entry) -> Unit,
    resetPriceInformation: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Balance(
            title = stringResource(R.string.coinview_price_label, data.assetName),
            price = data.priceFormattedWithFiatSymbol,
            percentageChangeData = PercentageChangeData(
                priceChange = data.priceChangeFormattedWithFiatSymbol,
                percentChange = data.percentChange,
                interval = stringResource(data.intervalName)
            ),
            endIcon = ImageResource.Remote(url = data.assetLogo, shape = CircleShape)
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            factory = { context ->
                ChartView(context).apply {
                    isChartLive = false
                    onEntryHighlighted = { entry ->
                        onChartEntryHighlighted(entry)
                    }
                    onActionPressDown = {
                        //                        analytics.logEvent(
                        //                            CoinViewAnalytics.ChartEngaged(
                        //                                origin = LaunchOrigin.COIN_VIEW,
                        //                                currency = assetTicker,
                        //                                timeInterval = stringPositionToTimeInterval(binding.chartControls.selectedItemIndex)
                        //                            )
                        //                        )
                    }
                    onScrubRelease = {
                        //                        analytics.logEvent(
                        //                            CoinViewAnalytics.ChartDisengaged(
                        //                                origin = LaunchOrigin.COIN_VIEW,
                        //                                currency = assetTicker,
                        //                                timeInterval = stringPositionToTimeInterval(binding.chartControls.selectedItemIndex)
                        //                            )
                        //                        )
                        //                        renderPriceInformation(
                        //                            prices24Hr,
                        //                            historicalGraphData,
                        //                            selectedFiat
                        //                        )
                        resetPriceInformation()
                    }
                    //                    shouldVibrate = localSettingsPrefs.isChartVibrationEnabled

                    fun HistoricalTimeSpan.toDatePattern(): String {
                        val PATTERN_HOURS = "HH:mm"
                        val PATTERN_DAY_HOUR = "HH:mm, EEE"
                        val PATTERN_DAY_HOUR_MONTH = "HH:mm d, MMM"
                        val PATTERN_DAY_MONTH_YEAR = "d MMM YYYY"
                        return when (this) {
                            HistoricalTimeSpan.DAY -> PATTERN_HOURS
                            HistoricalTimeSpan.WEEK -> PATTERN_DAY_HOUR
                            HistoricalTimeSpan.MONTH -> PATTERN_DAY_HOUR_MONTH
                            HistoricalTimeSpan.YEAR,
                            HistoricalTimeSpan.ALL_TIME,
                            -> PATTERN_DAY_MONTH_YEAR
                        }
                    }

                    datePattern = HistoricalTimeSpan.fromInt(0).toDatePattern()
                    fiatSymbol = "AA"/*state.selectedFiat.symbol*/
                    setData(data.chartData)
                }
            }
        )

        TabLayoutLive(
            items = listOf(
                stringResource(R.string.coinview_chart_tab_day),
                stringResource(R.string.coinview_chart_tab_week),
                stringResource(R.string.coinview_chart_tab_month),
                stringResource(R.string.coinview_chart_tab_year),
                stringResource(R.string.coinview_chart_tab_all)
            ),
            onItemSelected = { index ->
                //                selectedItemIndex = index
                //                onItemSelected(index)
            },
            selectedItemIndex = 0,
            showLiveIndicator = false
        )
    }
}

@Composable
fun AssetPriceError() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(AppTheme.dimensions.paddingLarge),
        contentAlignment = Alignment.Center
    ) {
        CardAlert(
            title = stringResource(R.string.coinview_chart_load_error_title),
            subtitle = stringResource(R.string.coinview_chart_load_error_subtitle),
            alertType = AlertType.Warning,
            isBordered = true,
            isDismissable = false
        )
    }
}

@Preview
@Composable
fun PreviewAssetPrice_Loading() {
    AssetPrice(CoinviewPriceState.Loading, {}, {})
}

@Preview
@Composable
fun PreviewAssetPrice_Data() {
    AssetPrice(
        CoinviewPriceState.Data(
            assetName = "Ethereum",
            assetLogo = "logo//",
            priceFormattedWithFiatSymbol = "$4,570.27",
            priceChangeFormattedWithFiatSymbol = "$969.25",
            percentChange = 5.58,
            intervalName = R.string.coinview_price_day,
            chartData = listOf(ChartEntry(1.4f, 43f), ChartEntry(3.4f, 4f)),
            selectedTimeSpan = HistoricalTimeSpan.DAY
        ),
        {},
        {}
    )
}

@Preview
@Composable
fun PreviewAssetPriceError() {
    AssetPriceError()
}