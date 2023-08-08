package com.blockchain.transactions.receive.accounts.composable

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.chrome.LocalNavControllerProvider
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.ActionTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.toStackedIcon
import com.blockchain.data.DataResource
import com.blockchain.image.LogoValue
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.analytics.analyticsProvider
import com.blockchain.stringResources.R
import com.blockchain.transactions.receive.ReceiveAnalyticsEvents
import com.blockchain.transactions.receive.accounts.ReceiveAccountType
import com.blockchain.transactions.receive.accounts.ReceiveAccountViewState
import com.blockchain.transactions.receive.accounts.ReceiveAccountsIntent
import com.blockchain.transactions.receive.accounts.ReceiveAccountsNavigation
import com.blockchain.transactions.receive.accounts.ReceiveAccountsViewModel
import com.blockchain.transactions.receive.accounts.ReceiveAccountsViewState
import com.blockchain.transactions.receive.navigation.ReceiveDestination
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ReceiveAccounts(
    viewModel: ReceiveAccountsViewModel = getViewModel(scope = payloadScope),
    onBackPressed: () -> Unit
) {
    val analytics = analyticsProvider()
    val navController = LocalNavControllerProvider.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val viewState: ReceiveAccountsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    val lifecycleOwner = LocalLifecycleOwner.current
    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(viewModel) {
        navEventsFlowLifecycleAware.collectLatest { navEvent ->
            when (navEvent) {
                is ReceiveAccountsNavigation.Detail -> {
                    navController.navigate(ReceiveDestination.AccountDetail)

                    analytics.logEvent(
                        ReceiveAnalyticsEvents.ReceiveAccountSelected(
                            navEvent.accountType,
                            navEvent.networkTicker
                        )
                    )
                }

                ReceiveAccountsNavigation.KycUpgrade -> {
                    navController.navigate(ReceiveDestination.KycUpgrade)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ReceiveAccountsIntent.LoadData)
    }

    ReceiveAccountsScreen(
        accounts = viewState.accounts,
        onAccountSelected = {
            keyboardController?.hide()
            viewModel.onIntent(ReceiveAccountsIntent.AccountSelected(id = it))
        },
        onSearchTermEntered = {
            viewModel.onIntent(ReceiveAccountsIntent.Search(term = it))
        },
        onBackPressed = onBackPressed
    )
}

@Composable
private fun ReceiveAccountsScreen(
    accounts: DataResource<Map<ReceiveAccountType, List<ReceiveAccountViewState>>>,
    onAccountSelected: (id: String) -> Unit,
    onSearchTermEntered: (term: String) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier.background(AppColors.background)
    ) {
        NavigationBar(
            title = stringResource(R.string.common_deposit),
            onBackButtonClick = onBackPressed,
        )

        when (accounts) {
            DataResource.Loading -> {
                ShimmerLoadingCard(
                    modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)
                )
            }

            is DataResource.Error -> {
                // todo
            }

            is DataResource.Data -> {
                ReceiveAccountsData(
                    accounts = accounts,
                    onSearchTermEntered = onSearchTermEntered,
                    onAccountSelected = onAccountSelected
                )
            }
        }
    }
}

@Composable
private fun ReceiveAccountsData(
    accounts: DataResource.Data<Map<ReceiveAccountType, List<ReceiveAccountViewState>>>,
    onSearchTermEntered: (term: String) -> Unit,
    onAccountSelected: (id: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        CancelableOutlinedSearch(
            onValueChange = onSearchTermEntered,
            placeholder = stringResource(R.string.search)
        )
    }

    LazyColumn {
        with(accounts.data) {
            forEach { (type, accounts) ->
                paddedItem(
                    paddingValues = {
                        PaddingValues(
                            horizontal = AppTheme.dimensions.standardSpacing,
                            vertical = AppTheme.dimensions.tinySpacing,
                        )
                    }
                ) {
                    Text(
                        text = stringResource(type.title()),
                        style = AppTheme.typography.body2,
                        color = AppColors.body
                    )
                }

                paddedRoundedCornersItems(
                    items = accounts,
                    paddingValues = {
                        PaddingValues(
                            start = AppTheme.dimensions.smallSpacing,
                            end = AppTheme.dimensions.smallSpacing,
                            bottom = AppTheme.dimensions.smallSpacing
                        )
                    }
                ) { account ->
                    ActionTableRow(
                        icon = account.icon.toStackedIcon(),
                        title = account.name,
                        subtitle = account.label,
                        tag = account.network,
                        onClick = { onAccountSelected(account.id) }
                    )
                }
            }
        }
    }
}

@StringRes
private fun ReceiveAccountType.title() = when (this) {
    ReceiveAccountType.Fiat -> R.string.payment_deposit
    ReceiveAccountType.Crypto -> R.string.common_crypto_deposit
}

@Preview
@Composable
private fun PreviewReceiveAccountsScreen() {
    ReceiveAccountsScreen(
        accounts = DataResource.Data(
            mapOf(
                ReceiveAccountType.Fiat to listOf(
                    ReceiveAccountViewState(
                        id = "", icon = LogoValue.SingleIcon(""), name = "USD", label = null, network = null
                    ),
                ).toImmutableList(),
                ReceiveAccountType.Crypto to listOf(
                    ReceiveAccountViewState(
                        id = "", icon = LogoValue.SingleIcon(""), name = "BTC", label = "btc", network = null
                    ),
                    ReceiveAccountViewState(
                        id = "", icon = LogoValue.SingleIcon(""), name = "ETH", label = "eth", network = "ethereum"
                    ),
                    ReceiveAccountViewState(
                        id = "", icon = LogoValue.SingleIcon(""), name = "USDC", label = "usdc", network = null
                    )
                ).toImmutableList()
            )
        ),
        {}, {}
    ) {}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewReceiveAccountsScreenDark() {
    PreviewReceiveAccountsScreen()
}
