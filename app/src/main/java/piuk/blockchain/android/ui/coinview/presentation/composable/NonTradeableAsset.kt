package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetTradeableState

@Composable
fun NonTradeableAsset(
    data: CoinviewAssetTradeableState
) {
    when (data) {
        CoinviewAssetTradeableState.Tradeable -> {
            Empty()
        }

        is CoinviewAssetTradeableState.NonTradeable -> {
            NonTradeableAssetData(data)
        }
    }
}

@Composable
fun NonTradeableAssetData(data: CoinviewAssetTradeableState.NonTradeable) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.dimensions.paddingLarge,
                vertical = AppTheme.dimensions.paddingMedium
            )
    ) {
        CardAlert(
            title = stringResource(R.string.coinview_not_tradeable_title, data.assetName, data.assetTicker),
            subtitle = stringResource(R.string.coinview_not_tradeable_subtitle, data.assetName),
            isDismissable = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNonTradeableAsset_Data() {
    NonTradeableAsset(
        CoinviewAssetTradeableState.NonTradeable(
            assetName = "Ethereum",
            assetTicker = "ETH"
        )
    )
}
