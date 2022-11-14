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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.alert.SnackbarAlert
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.core.price.HistoricalTimeSpan
import com.github.mikephil.charting.data.Entry
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetInfoState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAssetTradeableState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewBottomQuickActionsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewCenterQuickActionsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewIntent
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewSnackbarAlertState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewTotalBalanceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewModel
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewWatchlistState
import piuk.blockchain.android.ui.coinview.presentation.SimpleValue
import piuk.blockchain.android.ui.coinview.presentation.toModelState
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
            asset = state.asset,
            onContactSupportClick = {
                viewModel.onIntent(CoinviewIntent.ContactSupport)
            },
            price = state.assetPrice,
            onChartEntryHighlighted = { entry ->
                viewModel.onIntent(CoinviewIntent.UpdatePriceForChartSelection(entry))
            },
            resetPriceInformation = {
                viewModel.onIntent(CoinviewIntent.ResetPriceSelection)
            },
            onNewTimeSpanSelected = { timeSpan ->
                viewModel.onIntent(CoinviewIntent.NewTimeSpanSelected(timeSpan))
            },
            tradeable = state.tradeable,

            watchlist = state.watchlist,
            onWatchlistClick = {
                viewModel.onIntent(CoinviewIntent.ToggleWatchlist)
            },

            totalBalance = state.totalBalance,
            accounts = state.accounts,
            onAccountClick = { account ->
                viewModel.onIntent(CoinviewIntent.AccountSelected(account))
            },
            onLockedAccountClick = {
                viewModel.onIntent(CoinviewIntent.LockedAccountSelected)
            },
            quickActionsCenter = state.centerQuickAction,
            recurringBuys = state.recurringBuys,
            onRecurringBuyUpsellClick = {
                viewModel.onIntent(CoinviewIntent.RecurringBuysUpsell)
            },
            onRecurringBuyItemClick = { recurringBuyId ->
                viewModel.onIntent(CoinviewIntent.ShowRecurringBuyDetail(recurringBuyId))
            },
            quickActionsBottom = state.bottomQuickAction,
            onQuickActionClick = { quickAction ->
                viewModel.onIntent(CoinviewIntent.QuickActionSelected(quickAction.toModelState()))
            },
            assetInfo = state.assetInfo,
            onWebsiteClick = {
                viewModel.onIntent(CoinviewIntent.VisitAssetWebsite)
            },
            snackbarAlert = state.snackbarError
        )
    }
}

@Composable
fun CoinviewScreen(
    backOnClick: () -> Unit,

    asset: CoinviewAssetState,
    onContactSupportClick: () -> Unit,

    price: CoinviewPriceState,
    onChartEntryHighlighted: (Entry) -> Unit,
    resetPriceInformation: () -> Unit,
    onNewTimeSpanSelected: (HistoricalTimeSpan) -> Unit,

    tradeable: CoinviewAssetTradeableState,

    watchlist: CoinviewWatchlistState,
    onWatchlistClick: () -> Unit,

    totalBalance: CoinviewTotalBalanceState,

    accounts: CoinviewAccountsState,
    onAccountClick: (CoinviewAccount) -> Unit,
    onLockedAccountClick: () -> Unit,

    quickActionsCenter: CoinviewCenterQuickActionsState,

    recurringBuys: CoinviewRecurringBuysState,
    onRecurringBuyUpsellClick: () -> Unit,
    onRecurringBuyItemClick: (String) -> Unit,

    quickActionsBottom: CoinviewBottomQuickActionsState,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit,

    assetInfo: CoinviewAssetInfoState,
    onWebsiteClick: () -> Unit,

    snackbarAlert: CoinviewSnackbarAlertState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavigationBar(
                title = (asset as? CoinviewAssetState.Data)?.asset?.networkTicker ?: "",
                onBackButtonClick = backOnClick
            )

            when (asset) {
                CoinviewAssetState.Error -> {
                    UnknownAsset(onContactSupportClick = onContactSupportClick)
                }

                is CoinviewAssetState.Data -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1F)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            AssetPrice(
                                data = price,
                                assetTicker = asset.asset.networkTicker,
                                onChartEntryHighlighted = onChartEntryHighlighted,
                                resetPriceInformation = resetPriceInformation,
                                onNewTimeSpanSelected = onNewTimeSpanSelected
                            )

                            TotalBalance(
                                totalBalanceData = totalBalance,
                                watchlistData = watchlist,
                                assetTicker = asset.asset.networkTicker,
                                onWatchlistClick = onWatchlistClick
                            )

                            NonTradeableAsset(
                                data = tradeable
                            )

                            AssetAccounts(
                                data = accounts,
                                assetTicker = asset.asset.networkTicker,
                                onAccountClick = onAccountClick,
                                onLockedAccountClick = onLockedAccountClick
                            )

                            CenterQuickActions(
                                data = quickActionsCenter,
                                onQuickActionClick = onQuickActionClick
                            )

                            RecurringBuys(
                                data = recurringBuys,
                                assetTicker = asset.asset.networkTicker,
                                onRecurringBuyUpsellClick = onRecurringBuyUpsellClick,
                                onRecurringBuyItemClick = onRecurringBuyItemClick
                            )

                            AssetInfo(
                                data = assetInfo,
                                assetTicker = asset.asset.networkTicker,
                                onWebsiteClick = onWebsiteClick
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            BottomQuickActions(
                                data = quickActionsBottom,
                                onQuickActionClick = onQuickActionClick
                            )
                        }
                    }
                }
            }
        }

        if (snackbarAlert != CoinviewSnackbarAlertState.None) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                SnackbarAlert(
                    message = stringResource(snackbarAlert.message),
                    type = snackbarAlert.snackbarType
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

        asset = CoinviewAssetState.Data(CryptoCurrency.ETHER),
        onContactSupportClick = {},

        price = CoinviewPriceState.Loading,
        onChartEntryHighlighted = {},
        resetPriceInformation = {},
        onNewTimeSpanSelected = {},

        tradeable = CoinviewAssetTradeableState.Tradeable,

        watchlist = CoinviewWatchlistState.Loading,
        onWatchlistClick = {},

        totalBalance = CoinviewTotalBalanceState.Loading,
        accounts = CoinviewAccountsState.Loading,
        onAccountClick = {},
        onLockedAccountClick = {},

        quickActionsCenter = CoinviewCenterQuickActionsState.Loading,

        recurringBuys = CoinviewRecurringBuysState.Loading,
        onRecurringBuyUpsellClick = {},
        onRecurringBuyItemClick = {},

        quickActionsBottom = CoinviewBottomQuickActionsState.Loading,
        onQuickActionClick = {},

        assetInfo = CoinviewAssetInfoState.Loading,
        onWebsiteClick = {},

        snackbarAlert = CoinviewSnackbarAlertState.None
    )
}

@Preview(name = "CoinviewScreen unknown", showBackground = true)
@Composable
fun PreviewCoinviewScreen_Unknown() {
    CoinviewScreen(
        backOnClick = {},

        asset = CoinviewAssetState.Error,
        onContactSupportClick = {},

        price = CoinviewPriceState.Loading,
        onChartEntryHighlighted = {},
        resetPriceInformation = {},
        onNewTimeSpanSelected = {},

        tradeable = CoinviewAssetTradeableState.Tradeable,

        watchlist = CoinviewWatchlistState.Loading,
        onWatchlistClick = {},

        totalBalance = CoinviewTotalBalanceState.Loading,
        accounts = CoinviewAccountsState.Loading,
        onAccountClick = {},
        onLockedAccountClick = {},

        quickActionsCenter = CoinviewCenterQuickActionsState.Loading,

        recurringBuys = CoinviewRecurringBuysState.Loading,
        onRecurringBuyUpsellClick = {},
        onRecurringBuyItemClick = {},

        quickActionsBottom = CoinviewBottomQuickActionsState.Loading,
        onQuickActionClick = {},

        assetInfo = CoinviewAssetInfoState.Loading,
        onWebsiteClick = {},

        snackbarAlert = CoinviewSnackbarAlertState.None
    )
}

// todo move
@Composable
fun SimpleValue.value(): String {
    return when (this) {
        is SimpleValue.IntResValue -> stringResource(
            value,
            *(
                args.map {
                    when (it) {
                        is Int -> {
                            LocalContext.current.getStringMaybe(it)
                        }
                        else -> it.toString()
                    }
                }.toTypedArray()
                )
        )
        is SimpleValue.StringValue -> value
    }
}
