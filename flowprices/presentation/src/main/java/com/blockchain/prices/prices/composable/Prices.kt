package com.blockchain.prices.prices.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.prices.R
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import org.koin.androidx.compose.getViewModel

@Composable
fun Prices(
    viewModel: PricesViewModel = getViewModel(scope = payloadScope),
    listState: LazyListState
) {
    val viewState: PricesViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(PricesIntents.LoadAssetsAvailable)
        onDispose { }
    }

    PricesScreen(
        viewState.data,
        listState = listState,
        onSearchTermEntered = { term ->
            viewModel.onIntent(PricesIntents.FilterSearch(term = term))
        }
    )
}

@Composable
fun PricesScreen(
    data: DataResource<List<PriceItemViewState>>,
    listState: LazyListState,
    onSearchTermEntered: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        when (data) {
            DataResource.Loading -> {
            }
            is DataResource.Data -> {
                CryptoAssetsList(
                    cryptoPrices = data.data,
                    listState = listState,
                    onSearchTermEntered = onSearchTermEntered
                )
            }
            is DataResource.Error -> {
            }
        }
    }
}

@Composable
fun ColumnScope.CryptoAssetsList(
    cryptoPrices: List<PriceItemViewState>,
    listState: LazyListState,
    onSearchTermEntered: (String) -> Unit
) {
    CancelableOutlinedSearch(
        onValueChange = onSearchTermEntered,
        placeholder = stringResource(R.string.search)
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0XFFF1F2F7),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
        ) {
            itemsIndexed(
                items = cryptoPrices,
                itemContent = { index, cryptoAsset ->
                    BalanceChangeTableRow(
                        name = cryptoAsset.name,
                        subtitle = cryptoAsset.ticker,
                        value = cryptoAsset.currentPrice,
                        valueChange = cryptoAsset.delta,
                        contentStart = {
                            CustomStackedIcon(
                                icon = StackedIcon.SingleIcon(
                                    icon = ImageResource.Remote(cryptoAsset.logo)
                                )
                            )
                        },
                        onClick = { /*todo coinview*/ }
                    )
                    if (index < cryptoPrices.lastIndex) {
                        Divider(color = Color(0XFFF1F2F7))
                    }
                }
            )

            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
            }
        }

    }
}
