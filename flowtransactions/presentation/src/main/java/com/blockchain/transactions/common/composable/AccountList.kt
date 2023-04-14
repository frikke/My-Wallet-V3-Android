package com.blockchain.transactions.common.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.LazyRoundedCornersColumnIndexed
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceFiatAndCryptoTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.AccountUiElement

@Composable
fun AccountList(
    accounts: List<DataResource<AccountUiElement>>,
    onAccountClick: (AccountUiElement) -> Unit
) {
    LazyRoundedCornersColumnIndexed(
        modifier = Modifier.fillMaxSize(),
        items = accounts,
        rowContent = { accountData, index ->
            Column {
                if (accountData is DataResource.Data)
                    accountData.data.also { account ->
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
                                account.icon.size  > 1 -> {
                                    StackedIcon.OverlappingPair(
                                        ImageResource.Remote(account.icon[0]),
                                        ImageResource.Remote(account.icon[1])
                                    )
                                }
                                else -> StackedIcon.None
                            }
                        )
                    }
                else
                    ShimmerLoadingTableRow()

                if (index < accounts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.backgroundMuted
                    )
                }
            }
        }
    )
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
