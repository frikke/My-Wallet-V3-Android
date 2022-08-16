package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.charts.Balance
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState

@Composable
fun AssetPrice(
    balance: CoinviewPriceState
) {
    when (balance) {
        CoinviewPriceState.Loading -> {
            ShimmerLoadingTableRow(showIconLoader = false)
        }

        is CoinviewPriceState.Data -> {
            Balance(
                title = stringResource(R.string.coinview_price_label, balance.assetName),
                price = balance.priceFormattedWithFiatSymbol,
                percentageChangeData = balance.percentageChangeData,
                endIcon = ImageResource.Remote(url = balance.assetLogo, shape = CircleShape)
            )
        }
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
            )
        )
    )
}