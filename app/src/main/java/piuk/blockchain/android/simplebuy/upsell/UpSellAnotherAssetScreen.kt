package piuk.blockchain.android.simplebuy.upsell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.data.DataResource
import com.blockchain.prices.prices.PriceItemViewState
import info.blockchain.balance.CryptoCurrency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UpSellAnotherAsset(
    assets: ImmutableList<PriceItemViewState>,
    onBuyMostPopularAsset: (String) -> Unit,
    onMaybeLater: () -> Unit,
    onClose: () -> Unit
) {
    Card(shape = AppTheme.shapes.large, elevation = 0.dp, backgroundColor = AppTheme.colors.light) {
        Column {
            SheetHeader(
                shouldShowDivider = false,
                onClosePress = onClose,
                modifier = Modifier.background(color = AppTheme.colors.light)
            )

            Column(
                modifier = Modifier
                    .background(color = AppTheme.colors.light)
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TinyVerticalSpacer()

                SimpleText(
                    text = stringResource(com.blockchain.stringResources.R.string.asset_upsell_title),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                SmallVerticalSpacer()

                SimpleText(
                    text = stringResource(com.blockchain.stringResources.R.string.asset_upsell_subtitle),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre
                )

                StandardVerticalSpacer()

                MostPopularAssets(assets, onBuyMostPopularAsset = onBuyMostPopularAsset)

                LargeVerticalSpacer()

                MinimalButton(
                    text = stringResource(com.blockchain.stringResources.R.string.common_maybe_later),
                    onClick = onMaybeLater,
                    modifier = Modifier.fillMaxWidth(),
                    isTransparent = false
                )
            }

            StandardVerticalSpacer()
        }
    }
}

@Composable
fun MostPopularAssets(assets: ImmutableList<PriceItemViewState>, onBuyMostPopularAsset: (String) -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        Column {
            for (cryptoAsset in assets) {
                BalanceChangeTableRow(
                    data = cryptoAsset.data,
                    onClick = { onBuyMostPopularAsset(cryptoAsset.data.ticker) }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
private fun UpSellAnotherAssetScreenPreview() {
    AppTheme {
        UpSellAnotherAsset(
            assets = listOf(
                PriceItemViewState(
                    asset = CryptoCurrency.BTC,
                    data = BalanceChange(
                        name = "Bitcoin",
                        ticker = "BTC",
                        network = null,
                        logo = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                        delta = DataResource.Data(ValueChange.fromValue(2.5)),
                        currentPrice = DataResource.Data("$10,000"),
                        showRisingFastTag = false
                    )
                ),
                PriceItemViewState(
                    asset = CryptoCurrency.ETHER,
                    data = BalanceChange(
                        name = "Ethereum",
                        ticker = "ETH",
                        network = null,
                        logo = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                        delta = DataResource.Data(ValueChange.fromValue(2.5)),
                        currentPrice = DataResource.Data("$10,000"),
                        showRisingFastTag = false
                    )
                ),
                PriceItemViewState(
                    asset = CryptoCurrency.XLM,
                    data = BalanceChange(
                        name = "Litecoin",
                        ticker = "LTC",
                        network = null,
                        logo = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                        delta = DataResource.Data(ValueChange.fromValue(2.5)),
                        currentPrice = DataResource.Data("$10,000"),
                        showRisingFastTag = false
                    )
                ),
                PriceItemViewState(
                    asset = CryptoCurrency.XLM,
                    data = BalanceChange(
                        name = "Litecoin",
                        ticker = "LTC",
                        network = null,
                        logo = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                        delta = DataResource.Data(ValueChange.fromValue(2.5)),
                        currentPrice = DataResource.Data("$10,000"),
                        showRisingFastTag = false
                    )

                )
            ).toImmutableList(),
            onMaybeLater = { },
            onClose = { },
            onBuyMostPopularAsset = { }
        )
    }
}
