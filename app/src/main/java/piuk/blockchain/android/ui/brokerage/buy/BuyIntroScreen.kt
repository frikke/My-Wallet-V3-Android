package piuk.blockchain.android.ui.brokerage.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.control.NonCancelableOutlinedSearch
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.BlockedReason
import com.blockchain.presentation.customviews.EmptyStateView
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowScreen
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.composable.TopMovers
import com.blockchain.prices.prices.composable.TopMoversScreen
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.getViewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.CustomEmptyStateView
import piuk.blockchain.android.ui.dashboard.asPercentString

@Composable
fun BuyIntroScreen(
    buyViewState: BuyViewState,
    onSearchValueUpdated: (String) -> Unit,
    onListItemClicked: (AssetInfo) -> Unit,
    onEmptyStateClicked: (BlockedReason) -> Unit,
    onErrorRetryClicked: () -> Unit,
    onErrorContactSupportClicked: () -> Unit,
    startKycClicked: () -> Unit,
    toggleLoading: (Boolean) -> Unit
) {
    val pricesViewModel: PricesViewModel = getViewModel(scope = payloadScope)
    val pricesViewState: PricesViewState by pricesViewModel.viewState.collectAsStateLifecycleAware()
    DisposableEffect(key1 = pricesViewModel) {
        pricesViewModel.onIntent(PricesIntents.LoadData)
        onDispose { }
    }

    when (buyViewState) {
        BuyViewState.Loading -> {}
        is BuyViewState.ShowAssetList -> {
            when (pricesViewState.topMovers) {
                DataResource.Loading -> {
                    toggleLoading(true)
                }
                is DataResource.Error,
                is DataResource.Data -> {
                    toggleLoading(false)

                    val listItems = buyViewState.list
                    val listState = rememberLazyListState()

                    Column(
                        modifier = Modifier
                            .background(AppTheme.colors.light)
                    ) {
                        var searchedText by remember { mutableStateOf("") }

                        CancelableOutlinedSearch(
                            modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
                            placeholder = stringResource(R.string.search_coins_hint),
                            onValueChange = {
                                searchedText = it
                                onSearchValueUpdated(it)
                            }
                        )

                        SmallVerticalSpacer()

                        if (searchedText.isNotEmpty() && listItems.isEmpty()) {
                            SimpleText(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.assets_no_result),
                                style = ComposeTypographies.Body1,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.Centre
                            )
                        } else {
                            LazyColumn(
                                state = listState
                            ) {
                                item {
                                    TopMoversScreen(
                                        data = pricesViewState.topMovers,
                                        assetOnClick = { asset ->
                                            onListItemClicked(asset)
                                        }
                                    )
                                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                                }

                                paddedRoundedCornersItems(
                                    items = listItems,
                                    key = {
                                        it.asset.networkTicker
                                    },
                                    paddingValues = PaddingValues(horizontal = 16.dp)
                                ) { item ->
                                    BuyItem(
                                        buyItem = item,
                                        onClick = { onListItemClicked(item.asset) }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                                }
                            }
                        }
                    }
                }
            }
        }
        is BuyViewState.ShowEmptyState -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CustomEmptyStateView(context).apply {
                    title = R.string.account_restricted
                    descriptionText = buyViewState.description
                    icon = buyViewState.icon
                    ctaText = buyViewState.ctaText
                    ctaAction = { onEmptyStateClicked(buyViewState.reason) }
                }
            }
        )
        is BuyViewState.ShowError -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                EmptyStateView(context).apply {
                    setDetails(
                        action = onErrorRetryClicked,
                        onContactSupport = onErrorContactSupportClicked,
                        contactSupportEnabled = true
                    )
                }
            }
        )
        BuyViewState.ShowKyc -> KycUpgradeNowScreen(startKycClicked = startKycClicked)
    }
}

@Composable
fun BuyItem(
    buyItem: BuyCryptoItem,
    onClick: () -> Unit,
) {

    val accessAssetName = stringResource(R.string.accessibility_asset_name)

    TableRow(
        contentStart = {
            Image(
                imageResource = ImageResource.Remote(
                    buyItem.asset.logo
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                    .clip(CircleShape)
            )
        },

        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
                        end = dimensionResource(
                            id = com.blockchain.walletconnect.R.dimen.tiny_spacing
                        )
                    )
            ) {
                Text(
                    text = buyItem.asset.name,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title,
                    modifier = Modifier.semantics {
                        contentDescription = "$accessAssetName " + buyItem.asset.name
                    }
                )
                Row {
                    Text(
                        text = buyItem.price.toStringWithSymbol(),
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                horizontal = dimensionResource(id = com.blockchain.walletconnect.R.dimen.tiny_spacing)
                            ),
                        text = buyItem.percentageDelta.takeIf { !it.isNullOrNaN() }?.asPercentString() ?: "--",
                        style = AppTheme.typography.paragraph1,
                        color = buyItem.percentageDelta.takeIf { !it.isNullOrNaN() }?.let {
                            when {
                                it > 0 -> AppTheme.colors.success
                                it < 0 -> AppTheme.colors.error
                                else -> AppTheme.colors.primary
                            }
                        } ?: AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            Image(
                imageResource = ImageResource.Local(R.drawable.ic_chevron_end),
                modifier = Modifier.requiredSizeIn(
                    maxWidth = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    maxHeight = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                ),
            )
        },
        onContentClicked = onClick,
    )
    Divider(color = AppTheme.colors.light, thickness = 1.dp)
}

private fun Double?.isNullOrNaN(): Boolean {
    return this == null || this.isNaN()
}
