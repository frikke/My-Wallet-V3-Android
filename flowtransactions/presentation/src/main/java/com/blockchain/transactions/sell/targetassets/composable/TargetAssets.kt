package com.blockchain.transactions.sell.targetassets.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.FiatAccount
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.data.updateDataWith
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.prices.composable.SelectFiatAccountList
import com.blockchain.transactions.sell.SellService
import com.blockchain.transactions.sell.targetassets.TargetAssetsArgs
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.get

@Composable
fun TargetAssets(
    args: TargetAssetsArgs,
    sellService: SellService = get(scope = payloadScope),
    analytics: Analytics = get(),
    accountSelected: (
        fromAccount: CryptoAccountWithBalance,
        secondPassword: String?,
        toAccount: FiatAccount
    ) -> Unit,
    onClosePressed: () -> Unit
) {
    val sourceAccount = args.sourceAccount.data ?: return
    val secondPassword = args.secondPassword

    var state: DataResource<List<FiatAccount>> by remember { mutableStateOf(DataResource.Loading) }

    LaunchedEffect(Unit) {
        sellService.targetAccounts(sourceAccount.account)
            .collectLatest { data ->
                state = state.updateDataWith(data)
            }
    }

    TargetAssetsScreen(
        accounts = state,
        accountOnClick = { account ->
            accountSelected(sourceAccount, secondPassword, account)
//            analytics.logEvent(
//                SellAnalyticsEvents.DestinationAccountSelected(
//                    ticker = account.currency.networkTicker
//                )
//            )
            onClosePressed()
        },
        onBackPressed = onClosePressed
    )
}

@Composable
fun TargetAssetsScreen(
    accounts: DataResource<List<FiatAccount>>,
    accountOnClick: (item: FiatAccount) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(com.blockchain.stringResources.R.string.sell_targetasset_sell_for),
            onCloseClick = onBackPressed
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        SelectFiatAccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = accounts,
            onAccountClick = accountOnClick,
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewSelectTargetScreen() {
    TargetAssetsScreen(
        accounts = DataResource.Data(
            listOf(
//                BalanceChange(
//                    name = "Bitcoin",
//                    ticker = "BTC",
//                    network = null,
//                    logo = "",
//                    delta = DataResource.Data(ValueChange.fromValue(12.9)),
//                    currentPrice = DataResource.Data("122922"),
//                    showRisingFastTag = false
//                ),
//                BalanceChange(
//                    name = "Ethereum",
//                    ticker = "ETH",
//                    network = "Ethereum",
//                    logo = "",
//                    delta = DataResource.Data(ValueChange.fromValue(-2.9)),
//                    currentPrice = DataResource.Data("1222"),
//                    showRisingFastTag = false
//                )
            )
        ),
        accountOnClick = {},
        onBackPressed = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewSelectTargetScreen_WithFilter() {
    TargetAssetsScreen(
        accounts = DataResource.Data(
            listOf(
//                BalanceChange(
//                    name = "Bitcoin",
//                    ticker = "BTC",
//                    network = null,
//                    logo = "",
//                    delta = DataResource.Data(ValueChange.fromValue(12.9)),
//                    currentPrice = DataResource.Data("122922"),
//                    showRisingFastTag = false
//                ),
//                BalanceChange(
//                    name = "Ethereum",
//                    ticker = "ETH",
//                    network = "Ethereum",
//                    logo = "",
//                    delta = DataResource.Data(ValueChange.fromValue(-2.9)),
//                    currentPrice = DataResource.Data("1222"),
//                    showRisingFastTag = false
//                )
            )
        ),
        accountOnClick = {},
        onBackPressed = {}
    )
}
