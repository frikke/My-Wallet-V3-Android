package com.blockchain.transactions.swap.sourceaccounts.composable

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
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.accounts.composable.AccountList
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.transactions.swap.SwapAnalyticsEvents
import com.blockchain.transactions.swap.sourceaccounts.SourceAccountsIntent
import com.blockchain.transactions.swap.sourceaccounts.SourceAccountsNavigationEvent
import com.blockchain.transactions.swap.sourceaccounts.SourceAccountsViewModel
import com.blockchain.transactions.swap.sourceaccounts.SourceAccountsViewState
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun SourceAccounts(
    viewModel: SourceAccountsViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
    accountSelected: (CryptoAccountWithBalance) -> Unit,
    navigateToEnterSecondPassword: (CryptoAccountWithBalance) -> Unit,
    onBackPressed: () -> Unit
) {
    val viewState: SourceAccountsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(SourceAccountsIntent.LoadData)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { navEvent ->
            when (navEvent) {
                is SourceAccountsNavigationEvent.ConfirmSelection -> {
                    analytics.logEvent(
                        SwapAnalyticsEvents.SourceAccountSelected(
                            ticker = navEvent.account.account.currency.networkTicker
                        )
                    )
                    if (navEvent.requiresSecondPassword) {
                        navigateToEnterSecondPassword(navEvent.account)
                    } else {
                        accountSelected(navEvent.account)
                        onBackPressed()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(com.blockchain.stringResources.R.string.common_swap_from),
            onCloseClick = onBackPressed
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        AccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = viewState.accountList,
            onAccountClick = {
                viewModel.onIntent(SourceAccountsIntent.AccountSelected(it.id))
            },
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}
