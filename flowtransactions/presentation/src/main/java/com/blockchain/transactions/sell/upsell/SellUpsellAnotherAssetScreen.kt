package com.blockchain.transactions.sell.upsell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import com.blockchain.transactions.upsell.UpSellAnotherAssetDismissed
import com.blockchain.transactions.upsell.UpsellAnotherAssetScreen
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.get

@Composable
fun SellUpsellAnotherAssetScreen(
    assetCatalogue: AssetCatalogue = get(scope = payloadScope),
    analytics: Analytics = get(scope = payloadScope),
    assetJustSoldTicker: String,
    navigateToBuy: (AssetInfo) -> Unit,
    exitFlow: () -> Unit,
) {
    Column {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = "",
            onCloseClick = {
                analytics.logEvent(UpSellAnotherAssetDismissed)
                exitFlow()
            }
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        UpsellAnotherAssetScreen(
            title = stringResource(R.string.sell_asset_upsell_title),
            description = stringResource(R.string.sell_asset_upsell_subtitle),
            assetJustTransactedTicker = assetJustSoldTicker,
            onBuyMostPopularAsset = { ticker ->
                val asset = assetCatalogue.assetInfoFromNetworkTicker(ticker)
                if (asset != null) {
                    navigateToBuy(asset)
                } else {
                    exitFlow()
                }
            },
            onClose = exitFlow
        )
    }
}

@Preview
@Composable
private fun Preview() {
    SellUpsellAnotherAssetScreen(
        assetJustSoldTicker = "BTC",
        navigateToBuy = {},
        exitFlow = {}
    )
}
