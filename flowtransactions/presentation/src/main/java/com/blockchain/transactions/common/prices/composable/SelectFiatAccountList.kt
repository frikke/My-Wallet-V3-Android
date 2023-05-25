package com.blockchain.transactions.common.prices.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.blockchain.coincore.FiatAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.data.DataResource

@Composable
fun SelectFiatAccountList(
    modifier: Modifier = Modifier,
    accounts: DataResource<List<FiatAccount>>,
    onAccountClick: (FiatAccount) -> Unit,
    bottomSpacer: Dp? = null
) {
    when (accounts) {
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
                    items = accounts.data,
                    key = { it.currency.networkTicker },
                    animateItemPlacement = true,
                    content = { account ->
                        DefaultTableRow(
                            startTitle = account.currency.name,
                            startImageResource = ImageResource.Remote(account.currency.logo),
                            onClick = {
                                onAccountClick(account)
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
