package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.charts.ChartView
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.charts.Balance
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.control.TabLayoutLive
import com.blockchain.componentlib.system.LoadingChart
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState
import kotlin.random.Random

@Composable
fun AssetPrice(
    balance: CoinviewPriceState
) {
    when (balance) {
        CoinviewPriceState.Loading -> {
            AssetPriceInfoLoading()
        }

        is CoinviewPriceState.Data -> {
            AssetPriceInfoData(balance)
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
fun AssetPriceInfoData(data: CoinviewPriceState.Data) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Balance(
            title = stringResource(R.string.coinview_price_label, data.assetName),
            price = data.priceFormattedWithFiatSymbol,
            percentageChangeData = data.percentageChangeData,
            endIcon = ImageResource.Remote(url = data.assetLogo, shape = CircleShape)
        )

        AndroidView(
            factory = { context ->
                ChartView(context).apply {
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

@Preview
@Composable
fun PreviewAssetPrice_Loading() {
    AssetPrice(CoinviewPriceState.Loading)
}

@Preview
@Composable
fun PreviewAssetPrice_Data() {
    AssetPrice(
        CoinviewPriceState.Data(
            assetName = "Ethereum",
            assetLogo = "logo//",
            priceFormattedWithFiatSymbol = "$4,570.27",
            percentageChangeData = PercentageChangeData(
                priceChange = "$969.25",
                percentChange = 5.58,
                interval = "Past Hour"
            ),
            chartData = listOf()
        )
    )
}