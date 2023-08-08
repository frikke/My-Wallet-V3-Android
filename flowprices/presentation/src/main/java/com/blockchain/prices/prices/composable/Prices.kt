package com.blockchain.prices.prices.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.chrome.navigation.LocalAssetActionsNavigationProvider
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.icons.Fire
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tag.button.TagButtonRow
import com.blockchain.componentlib.tag.button.TagButtonValue
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.toImmutableList
import com.blockchain.koin.payloadScope
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.prices.prices.PricesAnalyticsEvents
import com.blockchain.prices.prices.PricesFilter
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesLoadStrategy
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.nameRes
import com.blockchain.prices.prices.percentAndPositionOf
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun Prices(
    viewModel: PricesViewModel = getViewModel(scope = payloadScope),
    listState: LazyListState,
    shouldTriggerRefresh: Boolean,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit
) {
    val assetActionsNavigation = LocalAssetActionsNavigationProvider.current

    val viewState: PricesViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(
            PricesIntents.LoadData(
                strategy = PricesLoadStrategy.All
            )
        )
        onDispose { }
    }

    DisposableEffect(shouldTriggerRefresh) {
        if (shouldTriggerRefresh) {
            viewModel.onIntent(PricesIntents.Refresh)
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
    ) {
        MenuOptionsScreen(
            openSettings = openSettings,
            launchQrScanner = launchQrScanner
        )

        PricesScreen(
            filters = viewState.availableFilters.toImmutableList(),
            selectedFilter = viewState.selectedFilter,
            data = viewState.allAssets.toImmutableList(),
            topMovers = viewState.filteredTopMovers(),
            listState = listState,
            onSearchTermEntered = { term ->
                viewModel.onIntent(PricesIntents.FilterSearch(term = term))
            },
            onFilterSelected = { filter ->
                viewModel.onIntent(PricesIntents.Filter(filter = filter))
            },
            onAssetClick = { asset ->
                assetActionsNavigation.coinview(asset)
            }
        )
    }
}

@Composable
fun PricesScreen(
    filters: ImmutableList<PricesFilter>,
    selectedFilter: PricesFilter,
    data: DataResource<ImmutableList<PriceItemViewState>>,
    topMovers: DataResource<ImmutableList<PriceItemViewState>>,
    listState: LazyListState,
    onSearchTermEntered: (String) -> Unit,
    onFilterSelected: (PricesFilter) -> Unit,
    onAssetClick: (AssetInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        when (data) {
            DataResource.Loading -> {
                ShimmerLoadingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.dimensions.smallSpacing)
                )
            }

            is DataResource.Data -> {
                PricesScreenData(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    cryptoPrices = data.data,
                    topMovers = topMovers,
                    listState = listState,
                    onSearchTermEntered = onSearchTermEntered,
                    onFilterSelected = onFilterSelected,
                    onAssetClick = onAssetClick
                )
            }

            is DataResource.Error -> {
            }
        }
    }
}

@Composable
fun ColumnScope.PricesScreenData(
    analytics: Analytics = get(),
    filters: ImmutableList<PricesFilter>,
    selectedFilter: PricesFilter,
    cryptoPrices: ImmutableList<PriceItemViewState>,
    topMovers: DataResource<ImmutableList<PriceItemViewState>>,
    listState: LazyListState,
    onSearchTermEntered: (String) -> Unit,
    onFilterSelected: (PricesFilter) -> Unit,
    onAssetClick: (AssetInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing)
    ) {
        CancelableOutlinedSearch(
            onValueChange = onSearchTermEntered,
            placeholder = stringResource(com.blockchain.stringResources.R.string.search)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        TagButtonRow(
            selected = selectedFilter,
            values = filters.map {
                TagButtonValue(obj = it, stringVal = stringResource(it.nameRes()))
            }.toImmutableList(),
            onClick = { filter -> onFilterSelected(filter) }
        )
    }

    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

    val scope = rememberCoroutineScope()
    DisposableEffect(key1 = selectedFilter) {
        scope.launch { listState.scrollToItem(index = 0) }
        onDispose { }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing))
    ) {
        (topMovers as? DataResource.Data)?.data?.let {
            if (it.isEmpty()) return@let

            paddedItem(
                paddingValues = {
                    PaddingValues(
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing,
                        bottom = AppTheme.dimensions.tinySpacing
                    )
                }
            ) {
                TableRowHeader(
                    title = stringResource(com.blockchain.stringResources.R.string.prices_top_movers),
                    icon = Icons.Filled.Fire
                        .withSize(AppTheme.dimensions.smallSpacing)
                        .withTint(AppTheme.colors.warningMuted)
                )
            }

            item {
                TopMoversScreen(
                    data = topMovers,
                    assetOnClick = { asset ->
                        onAssetClick(asset)

                        topMovers.percentAndPositionOf(asset)?.let { (percentageMove, position) ->
                            analytics.logEvent(
                                PricesAnalyticsEvents.TopMoverAssetClicked(
                                    ticker = asset.networkTicker,
                                    percentageMove = percentageMove,
                                    position = position
                                )
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
        }

        paddedRoundedCornersItems(
            items = cryptoPrices,
            key = {
                it.asset.networkTicker
            },
            paddingValues = {
                PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
            },
            content = { cryptoAsset ->
                BalanceChangeTableRow(
                    data = cryptoAsset.data,
                    onClick = { onAssetClick(cryptoAsset.asset) }
                )
            }
        )

        item {
            Spacer(modifier = Modifier.size(100.dp))
        }
    }
}

private fun PricesViewState.filteredTopMovers(): DataResource<ImmutableList<PriceItemViewState>> {
    val isTopMoversSupported = walletMode == WalletMode.CUSTODIAL && selectedFilter == PricesFilter.Tradable
    return if (isTopMoversSupported) {
        topMovers
    } else {
        DataResource.Data(emptyList())
    }.toImmutableList()
}
