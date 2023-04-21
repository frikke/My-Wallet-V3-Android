package com.blockchain.transactions.common.prices.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.data.DataResource

@Composable
fun SelectAssetPricesList(
    modifier: Modifier = Modifier,
    assets: DataResource<List<BalanceChange>>,
    onAccountClick: (BalanceChange) -> Unit,
    bottomSpacer: Dp? = null
) {
    when (assets) {
        DataResource.Loading -> {
            ShimmerLoadingCard(modifier = modifier)
        }
        is DataResource.Error -> {
            // todo
        }
        is DataResource.Data -> {
            LazyColumn(
                modifier = modifier.fillMaxSize()
            ) {
                roundedCornersItems(
                    items = assets.data,
                    content = { asset ->
                        BalanceChangeTableRow(
                            data = asset,
                            onClick = {
                            }
                        )
                    }
                )

                bottomSpacer?.let {
                    item {
                        Spacer(modifier = Modifier.size(bottomSpacer))
                    }
                }
            }
        }
    }
}
