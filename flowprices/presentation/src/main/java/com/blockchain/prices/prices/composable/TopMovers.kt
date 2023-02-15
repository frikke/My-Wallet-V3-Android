package com.blockchain.prices.prices.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.BalanceChangeSmallCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import info.blockchain.balance.AssetInfo
import org.koin.androidx.compose.getViewModel

@Composable
fun TopMovers(
    viewModel: PricesViewModel = getViewModel(scope = payloadScope),
    assetOnClick: (AssetInfo) -> Unit
) {
    val viewState: PricesViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(PricesIntents.LoadData)
        onDispose { }
    }

    TopMoversScreen(
        data = viewState.topMovers,
        assetOnClick = assetOnClick
    )
}

@Composable
fun TopMoversScreen(
    data: DataResource<List<PriceItemViewState>>,
    assetOnClick: (AssetInfo) -> Unit,
) {
    (data as? DataResource.Data)?.data?.let { topMovers ->
        if (topMovers.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AppTheme.dimensions.smallSpacing),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing)
            ) {
                items(topMovers) { assetPrice ->
                    BalanceChangeSmallCard(
                        name = assetPrice.name,
                        price = assetPrice.currentPrice,
                        valueChange = assetPrice.delta,
                        imageResource = ImageResource.Remote(assetPrice.logo),
                        onClick = { assetOnClick(assetPrice.asset) }
                    )
                }
            }
        }
    }
}
