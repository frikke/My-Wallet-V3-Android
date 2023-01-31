package com.blockchain.prices.prices.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.chrome.isScrollable
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.button.TagButtonRow
import com.blockchain.componentlib.tag.button.TagButtonValue
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.prices.R
import com.blockchain.prices.navigation.PricesNavigation
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.prices.prices.PricesFilter
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.nameRes
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.getViewModel

@Composable
fun Prices(
    viewModel: PricesViewModel = getViewModel(scope = payloadScope),
    listState: LazyListState,
    shouldTriggerRefresh: Boolean,
    pricesNavigation: PricesNavigation,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit
) {
    val viewState: PricesViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(PricesIntents.LoadData())
        onDispose { }
    }

    DisposableEffect(shouldTriggerRefresh) {
        if (shouldTriggerRefresh) {
            viewModel.onIntent(PricesIntents.Refresh)
        }
        onDispose { }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.smallSpacing)
        ) {
            MenuOptionsScreen(
                openSettings = openSettings,
                launchQrScanner = launchQrScanner
            )
        }

        PricesScreen(
            filters = viewState.availableFilters,
            selectedFilter = viewState.selectedFilter,
            data = viewState.data,
            listState = listState,
            onSearchTermEntered = { term ->
                viewModel.onIntent(PricesIntents.FilterSearch(term = term))
            },
            onFilterSelected = { filter ->
                viewModel.onIntent(PricesIntents.Filter(filter = filter))
            },
            onAssetClick = { asset ->
                pricesNavigation.coinview(asset)
            }
        )
    }
}

@Composable
fun PricesScreen(
    filters: List<PricesFilter>,
    selectedFilter: PricesFilter,
    data: DataResource<List<PriceItemViewState>>,
    listState: LazyListState,
    onSearchTermEntered: (String) -> Unit,
    onFilterSelected: (PricesFilter) -> Unit,
    onAssetClick: (AssetInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppTheme.dimensions.smallSpacing, end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
    ) {
        when (data) {
            DataResource.Loading -> {
                ShimmerLoadingCard()
            }
            is DataResource.Data -> {
                PricesScreenData(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    cryptoPrices = data.data,
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
    filters: List<PricesFilter>,
    selectedFilter: PricesFilter,
    cryptoPrices: List<PriceItemViewState>,
    listState: LazyListState,
    onSearchTermEntered: (String) -> Unit,
    onFilterSelected: (PricesFilter) -> Unit,
    onAssetClick: (AssetInfo) -> Unit
) {
    CancelableOutlinedSearch(
        onValueChange = onSearchTermEntered,
        placeholder = stringResource(R.string.search)
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

    TagButtonRow(
        selected = selectedFilter,
        values = filters.map { TagButtonValue(it, stringResource(it.nameRes())) },
        onClick = { filter -> onFilterSelected(filter) }
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing))
    ) {
        items(

            items = cryptoPrices,
            key = {
                it.asset.networkTicker
            },
            itemContent = { cryptoAsset ->
                BalanceChangeTableRow(
                    name = cryptoAsset.name,
                    subtitle = cryptoAsset.ticker,
                    networkTag = cryptoAsset.network,
                    value = cryptoAsset.currentPrice,
                    valueChange = cryptoAsset.delta,
                    contentStart = {
                        CustomStackedIcon(
                            icon = StackedIcon.SingleIcon(
                                icon = ImageResource.Remote(cryptoAsset.logo)
                            )
                        )
                    },
                    onClick = { onAssetClick(cryptoAsset.asset) }
                )
                if (cryptoPrices.last() != cryptoAsset) {
                    Divider(color = Color(0XFFF1F2F7))
                }
            }
        )

        /*
            If the list is scrollable we need to add a spacer to the bottom to ensure the last item is not obscured by
            the bottom nav FAB.
            In order to keep the rounded corners at the bottom we add a box with a rounded corner shape first and
            finally a spacer with the same background color as the outer layout
        */
        if (listState.isScrollable) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(AppTheme.dimensions.mediumSpacing)
                        .clip(
                            RoundedCornerShape(
                                bottomEnd = AppTheme.dimensions.mediumSpacing,
                                bottomStart = AppTheme.dimensions.mediumSpacing
                            )
                        )
                        .background(AppTheme.colors.background)
                )

                Spacer(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Color(0XFFF1F2F7))
                )
            }
        }
    }
}
