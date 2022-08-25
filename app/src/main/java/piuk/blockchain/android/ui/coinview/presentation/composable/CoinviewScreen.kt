package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.core.price.HistoricalTimeSpan
import com.github.mikephil.charting.data.Entry
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewIntents
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionsBottomState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionsCenterState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewTotalBalanceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewModel
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewState
import piuk.blockchain.android.ui.coinview.presentation.SimpleValue

@Composable
fun Coinview(
    viewModel: CoinviewViewModel,
    backOnClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: CoinviewViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        CoinviewScreen(
            backOnClick = backOnClick,
            networkTicker = state.assetName,

            price = state.assetPrice,
            onChartEntryHighlighted = { entry ->
                viewModel.onIntent(CoinviewIntents.UpdatePriceForChartSelection(entry))
            },
            resetPriceInformation = {
                viewModel.onIntent(CoinviewIntents.ResetPriceSelection)
            },
            onNewTimeSpanSelected = { timeSpan ->
                viewModel.onIntent(CoinviewIntents.NewTimeSpanSelected(timeSpan))
            },

            totalBalance = state.totalBalance,

            accounts = state.accounts,

            quickActionsCenter = state.quickActionCenter,

            recurringBuys = state.recurringBuys,
            onRecurringBuyUpsellClick = {
                viewModel.onIntent(CoinviewIntents.RecurringBuysUpsell)
            },
            onRecurringBuyItemClick = { recurringBuyId ->
                viewModel.onIntent(CoinviewIntents.ShowRecurringBuyDetail(recurringBuyId))
            },

            quickActionsBottom = state.quickActionBottom
        )
    }
}

@Composable
fun CoinviewScreen(
    backOnClick: () -> Unit,
    networkTicker: String,

    price: CoinviewPriceState,
    onChartEntryHighlighted: (Entry) -> Unit,
    resetPriceInformation: () -> Unit,
    onNewTimeSpanSelected: (HistoricalTimeSpan) -> Unit,

    totalBalance: CoinviewTotalBalanceState,

    accounts: CoinviewAccountsState,

    quickActionsCenter: CoinviewQuickActionsCenterState,

    recurringBuys: CoinviewRecurringBuysState,
    onRecurringBuyUpsellClick: () -> Unit,
    onRecurringBuyItemClick: (String) -> Unit,

    quickActionsBottom: CoinviewQuickActionsBottomState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NavigationBar(
            title = networkTicker,
            onBackButtonClick = backOnClick
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .verticalScroll(rememberScrollState()),
            ) {
                AssetPrice(
                    data = price,
                    onChartEntryHighlighted = onChartEntryHighlighted,
                    resetPriceInformation = resetPriceInformation,
                    onNewTimeSpanSelected = onNewTimeSpanSelected
                )

                TotalBalance(
                    data = totalBalance
                )

                AssetAccounts(
                    data = accounts
                )

                QuickActionsCenter(
                    data = quickActionsCenter
                )

                RecurringBuys(
                    data = recurringBuys,
                    onRecurringBuyUpsellClick = onRecurringBuyUpsellClick,
                    onRecurringBuyItemClick = onRecurringBuyItemClick
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                QuickActionsBottom(
                    data = quickActionsBottom
                )
            }
        }

    }
}

/**
 * For when no view is needed todo maybe moved to compose module
 */
@Composable
fun Empty() {
}

@Preview(name = "CoinviewScreen", showBackground = true)
@Composable
fun PreviewCoinviewScreen() {
    CoinviewScreen(
        backOnClick = {},
        networkTicker = "ETH",
        price = CoinviewPriceState.Loading,
        onChartEntryHighlighted = {},
        resetPriceInformation = {},
        onNewTimeSpanSelected = {},
        totalBalance = CoinviewTotalBalanceState.Loading,
        accounts = CoinviewAccountsState.Loading,
        quickActionsCenter = CoinviewQuickActionsCenterState.Loading,
        recurringBuys = CoinviewRecurringBuysState.Loading,
        onRecurringBuyUpsellClick = {},
        onRecurringBuyItemClick = {},
        quickActionsBottom = CoinviewQuickActionsBottomState.Loading
    )
}

// todo move
@Composable
fun SimpleValue.value(): String {
    return when (this) {
        is SimpleValue.IntResValue -> stringResource(value, *args.toTypedArray())
        is SimpleValue.StringValue -> value
    }
}
