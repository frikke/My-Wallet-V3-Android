package com.blockchain.transactions.common.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceFiatAndCryptoTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.AccountUiElement

@Composable
fun AccountList(
    modifier: Modifier = Modifier,
    accounts: DataResource<List<AccountUiElement>>,
    onAccountClick: (AccountUiElement) -> Unit,
    bottomSpacer: Dp? = null
) {
    when (accounts) {
        DataResource.Loading -> {
            ShimmerLoadingCard(modifier = modifier)
        }
        is DataResource.Error -> {
            //todo
        }
        is DataResource.Data -> {
            LazyColumn(
                modifier = modifier.fillMaxSize()
            ) {
                roundedCornersItems(
                    items = accounts.data,
                    content = { account ->
                        BalanceFiatAndCryptoTableRow(
                            title = account.title,
                            valueCrypto = account.valueCrypto,
                            valueFiat = account.valueFiat,
                            onClick = { onAccountClick(account) },
                            icon = when {
                                account.icon.size == 1 -> {
                                    StackedIcon.SingleIcon(
                                        ImageResource.Remote(account.icon[0])
                                    )
                                }
                                account.icon.size > 1 -> {
                                    StackedIcon.OverlappingPair(
                                        ImageResource.Remote(account.icon[0]),
                                        ImageResource.Remote(account.icon[1])
                                    )
                                }
                                else -> StackedIcon.None
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

/*
@Preview(showBackground = true)
@Composable
private fun AccountListPreview() {
    AppTheme {
        AccountList(
            accounts = listOf(
                AccountUiElement(
                    "Bitcoin",
                    "BTC",
                    "0.04936855 BTC",
                    "\$1,000.00",
                    icon = StackedIcon.SingleIcon(
                        ImageResource.Local(
                            R.drawable.ic_eth
                        )
                    )
                ),
                AccountUiElement(
                    "Bitcoin",
                    "BTC",
                    "0.04936855 BTC",
                    "\$1,000.00",
                    icon = StackedIcon.SingleIcon(
                        ImageResource.Local(
                            R.drawable.ic_eth
                        )
                    )
                ),
                AccountUiElement(
                    "Bitcoin",
                    "BTC",
                    "0.04936855 BTC",
                    "\$1,000.00",
                    icon = StackedIcon.SingleIcon(
                        ImageResource.Local(
                            R.drawable.ic_eth
                        )
                    )
                )
            ),
            onAccountClick = {_ -> }
        )
    }
}*/
