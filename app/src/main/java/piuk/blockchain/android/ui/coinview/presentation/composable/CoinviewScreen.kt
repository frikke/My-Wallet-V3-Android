package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.componentlib.alert.SnackbarAlert
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.core.price.HistoricalTimeSpan
import com.github.mikephil.charting.data.Entry
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetInfoState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewIntents
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionsBottomState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionsCenterState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewSnackbarErrorState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewTotalBalanceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewModel
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewState
import piuk.blockchain.android.ui.coinview.presentation.SimpleValue
import piuk.blockchain.android.util.getStringMaybe

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
            onAccountClick = { account ->
                viewModel.onIntent(CoinviewIntents.AccountSelected(account))
            },

            quickActionsCenter = state.quickActionCenter,

            recurringBuys = state.recurringBuys,
            onRecurringBuyUpsellClick = {
                viewModel.onIntent(CoinviewIntents.RecurringBuysUpsell)
            },
            onRecurringBuyItemClick = { recurringBuyId ->
                viewModel.onIntent(CoinviewIntents.ShowRecurringBuyDetail(recurringBuyId))
            },

            quickActionsBottom = state.quickActionBottom,

            assetInfo = state.assetInfo,
            onWebsiteClick = {},

            snackbar = state.snackbarError
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
    onAccountClick: (BlockchainAccount) -> Unit,

    quickActionsCenter: CoinviewQuickActionsCenterState,

    recurringBuys: CoinviewRecurringBuysState,
    onRecurringBuyUpsellClick: () -> Unit,
    onRecurringBuyItemClick: (String) -> Unit,

    quickActionsBottom: CoinviewQuickActionsBottomState,

    assetInfo: CoinviewAssetInfoState,
    onWebsiteClick: () -> Unit,

    snackbar: CoinviewSnackbarErrorState
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                        data = accounts,
                        onAccountClick = onAccountClick
                    )

                    QuickActionsCenter(
                        data = quickActionsCenter
                    )

                    RecurringBuys(
                        data = recurringBuys,
                        onRecurringBuyUpsellClick = onRecurringBuyUpsellClick,
                        onRecurringBuyItemClick = onRecurringBuyItemClick
                    )

                    AssetInfo(
                        data = assetInfo,
                        onWebsiteClick = onWebsiteClick
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    QuickActionsBottom(
                        data = quickActionsBottom
                    )
                }
            }
        }

        if(snackbar != CoinviewSnackbarErrorState.None){
            SnackbarAlert(
                message = stringResource(snackbar.message),
                type = snackbar.snackbarType
            )
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
        onAccountClick = {},

        quickActionsCenter = CoinviewQuickActionsCenterState.Loading,
        recurringBuys = CoinviewRecurringBuysState.Loading,
        onRecurringBuyUpsellClick = {},
        onRecurringBuyItemClick = {},
        quickActionsBottom = CoinviewQuickActionsBottomState.Loading,
        assetInfo = CoinviewAssetInfoState.Loading,
        onWebsiteClick = {},

        snackbar = CoinviewSnackbarErrorState.None
    )
}

// todo move
@Composable
fun SimpleValue.value(): String {
    return when (this) {
        is SimpleValue.IntResValue -> stringResource(
            value,
            *(args.map {
                when (it) {
                    is Int -> {
                        LocalContext.current.getStringMaybe(it)
                    }
                    else -> it.toString()
                }
            }.toTypedArray())
        )
        is SimpleValue.StringValue -> value
    }
}
