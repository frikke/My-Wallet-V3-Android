package com.blockchain.transactions.sell.upsell

import androidx.compose.foundation.background
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
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import com.blockchain.transactions.upsell.buy.UpsellBuyDismissed
import com.blockchain.transactions.upsell.buy.UpsellBuyScreen
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.get

@Composable
fun UpsellBuyAfterSell(
    assetCatalogue: AssetCatalogue = get(scope = payloadScope),
    analytics: Analytics = get(scope = payloadScope),
    assetJustSoldTicker: String,
    navigateToBuy: (AssetInfo) -> Unit,
    exitFlow: () -> Unit,
) {
    UpsellBuyAfterSellScreen(
        assetJustSoldTicker = assetJustSoldTicker,
        onBuyMostPopularAsset = { ticker ->
            val asset = assetCatalogue.assetInfoFromNetworkTicker(ticker)
            if (asset != null) {
                navigateToBuy(asset)
            } else {
                exitFlow()
            }
        },
        onCloseClick = {
            analytics.logEvent(UpsellBuyDismissed)
            exitFlow()
        },
        exitFlow = exitFlow
    )
}

@Composable
fun UpsellBuyAfterSellScreen(
    assetJustSoldTicker: String,
    onBuyMostPopularAsset: (String) -> Unit,
    onCloseClick: () -> Unit,
    exitFlow: () -> Unit,
) {
    Column(
        modifier = Modifier.background(AppColors.background)
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = "",
            onCloseClick = onCloseClick
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        UpsellBuyScreen(
            title = stringResource(R.string.sell_asset_upsell_title),
            description = stringResource(R.string.sell_asset_upsell_subtitle),
            assetJustTransactedTicker = assetJustSoldTicker,
            onBuyMostPopularAsset = onBuyMostPopularAsset,
            analytics = previewAnalytics,
            onClose = exitFlow
        )
    }
}

@Preview
@Composable
private fun PreviewUpsellBuyAfterSellScreen() {
    UpsellBuyAfterSellScreen(
        assetJustSoldTicker = "BTC",
        onBuyMostPopularAsset = {}, onCloseClick = {}, exitFlow = {}
    )
}
