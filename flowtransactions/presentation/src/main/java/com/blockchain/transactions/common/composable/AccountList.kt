package com.blockchain.transactions.common.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.system.LazyRoundedCornersColumnIndexed
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceFiatAndCryptoTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.AccountUiElement

@Composable
fun AccountList(
    accounts: DataResource<List<AccountUiElement>>,
    onAccountClick: (AccountUiElement) -> Unit
) {
    when (accounts) {
        DataResource.Loading -> {
            ShimmerLoadingCard()
        }
        is DataResource.Error -> {
            //todo
        }
        is DataResource.Data -> {
            LazyRoundedCornersColumnIndexed(
                modifier = Modifier.fillMaxSize(),
                items = accounts.data,
                rowContent = { account, index ->
                    Column {
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

                        if (index < accounts.data.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.backgroundMuted
                            )
                        }
                    }
                }
            )
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
