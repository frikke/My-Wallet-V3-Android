package com.blockchain.prices.prices.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.BalanceChangeSmallCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.prices.prices.PriceItemViewState
import info.blockchain.balance.AssetInfo
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TopMoversScreen(
    data: DataResource<ImmutableList<PriceItemViewState>>,
    assetOnClick: (AssetInfo) -> Unit
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
                        name = assetPrice.data.ticker,
                        price = assetPrice.data.currentPrice,
                        valueChange = assetPrice.data.delta,
                        imageResource = ImageResource.Remote(assetPrice.data.logo),
                        onClick = { assetOnClick(assetPrice.asset) }
                    )
                }
            }
        }
    }
}
