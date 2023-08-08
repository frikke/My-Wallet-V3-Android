package com.blockchain.transactions.swap.targetaccounts.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.accounts.AccountUiElement
import com.blockchain.transactions.common.accounts.composable.AccountList
import com.blockchain.transactions.swap.SwapAnalyticsEvents
import com.blockchain.transactions.swap.targetaccounts.SwapTargetAccountsIntent
import com.blockchain.transactions.swap.targetaccounts.SwapTargetAccountsNavigationEvent
import com.blockchain.transactions.swap.targetaccounts.SwapTargetAccountsViewModel
import com.blockchain.transactions.swap.targetaccounts.SwapTargetAccountsViewState
import com.blockchain.walletmode.WalletMode
import java.io.Serializable
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

data class SwapTargetAccountsArgs(
    val sourceTicker: String,
    val targetTicker: String,
    val mode: WalletMode
) : Serializable

@Composable
fun SwapTargetAccounts(
    sourceTicker: String,
    targetTicker: String,
    mode: WalletMode,
    viewModel: SwapTargetAccountsViewModel = getViewModel(
        scope = payloadScope,
        key = sourceTicker + targetTicker,
        parameters = { parametersOf(sourceTicker, targetTicker, mode) }
    ),
    analytics: Analytics = get(),
    accountSelected: (CryptoAccount) -> Unit,
    onClosePressed: () -> Unit,
    onBackPressed: () -> Unit
) {
    val viewState: SwapTargetAccountsViewState by viewModel.viewState.collectAsStateLifecycleAware()
    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(SwapTargetAccountsIntent.LoadData)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { navEvent ->
            when (navEvent) {
                is SwapTargetAccountsNavigationEvent.ConfirmSelection -> {
                    accountSelected(navEvent.account)
                    analytics.logEvent(
                        SwapAnalyticsEvents.DestinationAccountSelected(
                            ticker = navEvent.account.currency.networkTicker
                        )
                    )
                    onClosePressed()
                }
            }
        }
    }

    TargetAccountsScreen(
        targetTicker = targetTicker,
        accounts = viewState.accountList,
        accountSelected = {
            viewModel.onIntent(SwapTargetAccountsIntent.AccountSelected(it.id))
        },
        onClosePressed = onClosePressed,
        onBackPressed = onBackPressed
    )
}

@Composable
private fun TargetAccountsScreen(
    targetTicker: String,
    accounts: DataResource<List<AccountUiElement>>,
    accountSelected: (AccountUiElement) -> Unit,
    onClosePressed: () -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(com.blockchain.stringResources.R.string.common_swap_select_account, targetTicker),
            backOnClick = onBackPressed,
            onCloseClick = onClosePressed
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        AccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = accounts,
            onAccountClick = accountSelected,
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}
