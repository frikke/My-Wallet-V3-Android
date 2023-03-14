package com.dex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.tablerow.NonCustodialAssetBalanceTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import com.dex.domain.DexAccount
import info.blockchain.balance.isLayer2Token
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectSourceAccountBottomSheet(
    closeClicked: () -> Unit,
    viewModel: DexSourceAccountViewModel = getViewModel(scope = payloadScope)
) {
    val viewState: SourceAccountSelectionViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(SourceAccountIntent.LoadSourceAccounts)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundMuted)
    ) {
        SheetFloatingHeader(
            icon =
            StackedIcon.None,
            title = stringResource(id = R.string.select_token),
            onCloseClick = closeClicked
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SourceAccountsSelection(
                accounts = viewState.accounts,
                onAccountSelected = {
                    viewModel.onIntent(SourceAccountIntent.OnAccountSelected(it))
                    closeClicked()
                },
                onSearchTermUpdated = {
                    viewModel.onIntent(SourceAccountIntent.Search(it))
                }
            )
        }
    }
}

@Composable
fun SourceAccountsSelection(
    accounts: List<DexAccount>,
    onAccountSelected: (DexAccount) -> Unit,
    onSearchTermUpdated: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CancelableOutlinedSearch(
            onValueChange = {
                onSearchTermUpdated(it)
            },
            placeholder = stringResource(R.string.search)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing))
        ) {
            roundedCornersItems(
                items = accounts,
                key = {
                    it.currency.networkTicker
                },
                content = { dexAccount ->
                    NonCustodialAssetBalanceTableRow(
                        title = dexAccount.currency.name,
                        subtitle = dexAccount.currency.takeIf { it.isLayer2Token }?.coinNetwork?.shortName ?: "",
                        valueCrypto = dexAccount.balance.toStringWithSymbol(),
                        valueFiat = dexAccount.fiatBalance.toStringWithSymbol(),
                        icon = StackedIcon.SingleIcon(
                            icon = ImageResource.Remote(dexAccount.currency.logo)
                        ),
                        onClick = {
                            onAccountSelected(dexAccount)
                        }
                    )
                    if (accounts.last() != dexAccount) {
                        Divider(color = Color(0XFFF1F2F7))
                    }
                }
            )
        }
    }
}
