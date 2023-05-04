package com.blockchain.transactions.swap.targetassets.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.betternavigation.NavContext
import com.blockchain.betternavigation.navigateTo
import com.blockchain.chrome.titleIcon
import com.blockchain.chrome.titleSuperApp
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.button.TagButtonRow
import com.blockchain.componentlib.tag.button.TagButtonValue
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.prices.composable.SelectAssetPricesList
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.SwapGraph
import com.blockchain.transactions.swap.targetassets.SelectTargetIntent
import com.blockchain.transactions.swap.targetassets.SelectTargetViewModel
import com.blockchain.transactions.swap.targetassets.SelectTargetViewState
import com.blockchain.transactions.swap.targetassets.TargetAssetNavigationEvent
import com.blockchain.transactions.swap.targetaccounts.composable.SelectTargetAccountArgs
import com.blockchain.walletmode.WalletMode
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SelectTargetAsset(
    sourceTicker: String,
    viewModel: SelectTargetViewModel = getViewModel(
        scope = payloadScope,
        key = sourceTicker,
        parameters = { parametersOf(sourceTicker) }
    ),
    accountSelected: (CryptoAccount) -> Unit,
    navContextProvider: () -> NavContext,
    onClosePressed: () -> Unit
) {
    val viewState: SelectTargetViewState by viewModel.viewState.collectAsStateLifecycleAware()
    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(SelectTargetIntent.LoadData)
        onDispose { }
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { navEvent ->
            when (navEvent) {
                is TargetAssetNavigationEvent.ConfirmSelection -> {
                    accountSelected(navEvent.account)
                    onClosePressed()
                }

                is TargetAssetNavigationEvent.SelectAccount -> {
                    check(viewState.selectedModeFilter != null)
                    navContextProvider().navigateTo(
                        SwapGraph.TargetAccount,
                        SelectTargetAccountArgs(
                            sourceTicker = sourceTicker,
                            targetTicker = navEvent.ofTicker,
                            mode = viewState.selectedModeFilter!!
                        )
                    )
                }
            }
        }
    }

    SelectTargetScreen(
        onSearchTermEntered = { term ->
            viewModel.onIntent(SelectTargetIntent.FilterSearch(term = term))
        },
        showModeFilter = viewState.showModeFilter,
        selectedMode = viewState.selectedModeFilter,
        onFilterSelected = {
            viewModel.onIntent(SelectTargetIntent.ModeFilterSelected(it))
        },
        assets = viewState.prices,
        accountOnClick = {
            viewModel.onIntent(SelectTargetIntent.AssetSelected(ticker = it.ticker))
        },
        onBackPressed = onClosePressed
    )
}

@Composable
fun SelectTargetScreen(
    onSearchTermEntered: (String) -> Unit,
    showModeFilter: Boolean,
    selectedMode: WalletMode?,
    onFilterSelected: (WalletMode) -> Unit,
    assets: DataResource<List<BalanceChange>>,
    accountOnClick: (item: BalanceChange) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_swap_to),
            onCloseClick = onBackPressed
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        CancelableOutlinedSearch(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            onValueChange = onSearchTermEntered,
            placeholder = stringResource(R.string.search)
        )

        if (showModeFilter && selectedMode != null) {
            TagButtonRow(
                modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
                selected = selectedMode,
                values = WalletMode.values().toList().reversed().map {
                    TagButtonValue(obj = it, icon = it.titleIcon(), stringVal = stringResource(it.titleSuperApp()))
                }.toImmutableList(),
                onClick = onFilterSelected
            )
        } else {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
        }

        SelectAssetPricesList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            assets = assets,
            onAccountClick = accountOnClick,
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewSelectTargetScreen() {
    SelectTargetScreen(
        onSearchTermEntered = {},
        showModeFilter = false,
        selectedMode = WalletMode.CUSTODIAL,
        onFilterSelected = {},
        assets = DataResource.Data(
            listOf(
                BalanceChange(
                    name = "Bitcoin",
                    ticker = "BTC",
                    network = null,
                    logo = "",
                    delta = DataResource.Data(ValueChange.fromValue(12.9)),
                    currentPrice = DataResource.Data("122922"),
                    showRisingFastTag = false
                ),
                BalanceChange(
                    name = "Ethereum",
                    ticker = "ETH",
                    network = "Ethereum",
                    logo = "",
                    delta = DataResource.Data(ValueChange.fromValue(-2.9)),
                    currentPrice = DataResource.Data("1222"),
                    showRisingFastTag = false
                )
            )
        ),
        accountOnClick = {},
        onBackPressed = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewSelectTargetScreen_WithFilter() {
    SelectTargetScreen(
        onSearchTermEntered = {},
        showModeFilter = true,
        selectedMode = WalletMode.NON_CUSTODIAL,
        onFilterSelected = {},
        assets = DataResource.Data(
            listOf(
                BalanceChange(
                    name = "Bitcoin",
                    ticker = "BTC",
                    network = null,
                    logo = "",
                    delta = DataResource.Data(ValueChange.fromValue(12.9)),
                    currentPrice = DataResource.Data("122922"),
                    showRisingFastTag = false
                ),
                BalanceChange(
                    name = "Ethereum",
                    ticker = "ETH",
                    network = "Ethereum",
                    logo = "",
                    delta = DataResource.Data(ValueChange.fromValue(-2.9)),
                    currentPrice = DataResource.Data("1222"),
                    showRisingFastTag = false
                )
            )
        ),
        accountOnClick = {},
        onBackPressed = {},
    )
}
