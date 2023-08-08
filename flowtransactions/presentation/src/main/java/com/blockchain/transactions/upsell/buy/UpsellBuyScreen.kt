package com.blockchain.transactions.upsell.buy

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.transactions.upsell.buy.viewmodel.UpsellBuyIntent
import com.blockchain.transactions.upsell.buy.viewmodel.UpsellBuyViewModel
import com.blockchain.transactions.upsell.buy.viewmodel.UpsellBuyViewState
import info.blockchain.balance.CryptoCurrency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun UpsellBuyScreen(
    assetJustTransactedTicker: String,
    title: String,
    description: String,
    viewModel: UpsellBuyViewModel = getViewModel(
        scope = payloadScope,
        parameters = { parametersOf(assetJustTransactedTicker) }
    ),
    analytics: Analytics = get(),
    onBuyMostPopularAsset: (String) -> Unit,
    onClose: () -> Unit
) {
    val viewState: UpsellBuyViewState by viewModel.viewState.collectAsStateLifecycleAware()
    val state = viewState

    LaunchedEffect(Unit) {
        viewModel.onIntent(UpsellBuyIntent.LoadData)
        analytics.logEvent(UpsellBuyViewed)
    }

    Column {
        when {
            state.isLoading -> {
                ShimmerLoadingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.dimensions.smallSpacing)
                )
            }
            state.assetsToUpSell is DataResource.Data -> {
                Content(
                    title = title,
                    description = description,
                    assets = state.assetsToUpSell.data,
                    onBuyMostPopularAsset = { currency ->
                        analytics.logEvent(UpsellBuyMostPopularClicked(currency = currency))
                        onBuyMostPopularAsset(currency)
                    },
                    onMaybeLater = {
                        analytics.logEvent(UpsellBuyMaybeLaterClicked)
                        viewModel.onIntent(UpsellBuyIntent.DismissUpsell)
                        onClose()
                    },
                )
            }
            else -> {
            }
        }
    }
}

@Composable
private fun Content(
    title: String,
    description: String,
    assets: ImmutableList<PriceItemViewState>,
    onBuyMostPopularAsset: (String) -> Unit,
    onMaybeLater: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(color = AppTheme.colors.background)
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.standardSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TinyVerticalSpacer()

        SimpleText(
            text = title,
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SmallVerticalSpacer()

        SimpleText(
            text = description,
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        StandardVerticalSpacer()

        MostPopularAssets(assets, onBuyMostPopularAsset = onBuyMostPopularAsset)

        LargeVerticalSpacer()

        MinimalPrimaryButton(
            text = stringResource(com.blockchain.stringResources.R.string.common_maybe_later),
            onClick = onMaybeLater,
            modifier = Modifier.fillMaxWidth()
        )

        StandardVerticalSpacer()
    }
}

@Composable
fun MostPopularAssets(assets: ImmutableList<PriceItemViewState>, onBuyMostPopularAsset: (String) -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.backgroundSecondary,
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

@Preview
@Composable
private fun UpSellAnotherAssetScreenPreview() {
    AppTheme {
        Content(
            title = "title",
            description = "",
            assets = listOf(
                PriceItemViewState(
                    asset = CryptoCurrency.BTC,
                    data = BalanceChange(
                        name = "Bitcoin",
                        ticker = "BTC",
                        network = null,
                        logo = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                        nativeAssetLogo = null,
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
                        nativeAssetLogo = null,
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
                        nativeAssetLogo = null,
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
                        nativeAssetLogo = null,
                        delta = DataResource.Data(ValueChange.fromValue(2.5)),
                        currentPrice = DataResource.Data("$10,000"),
                        showRisingFastTag = false
                    )

                )
            ).toImmutableList(),
            onMaybeLater = { },
            onBuyMostPopularAsset = { }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpSellAnotherAssetScreenPreviewDark() {
    UpSellAnotherAssetScreenPreview()
}
