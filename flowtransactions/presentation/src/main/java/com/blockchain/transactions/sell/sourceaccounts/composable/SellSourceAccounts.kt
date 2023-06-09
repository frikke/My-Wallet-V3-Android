package com.blockchain.transactions.sell.sourceaccounts.composable

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
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.accounts.composable.AccountList
import com.blockchain.transactions.sell.SellAnalyticsEvents
import com.blockchain.transactions.sell.sourceaccounts.SellSourceAccountsIntent
import com.blockchain.transactions.sell.sourceaccounts.SellSourceAccountsNavigationEvent
import com.blockchain.transactions.sell.sourceaccounts.SellSourceAccountsViewModel
import com.blockchain.transactions.sell.sourceaccounts.SellSourceAccountsViewState
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun SellSourceAccounts(
    viewModel: SellSourceAccountsViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
    accountSelected: (CryptoAccountWithBalance) -> Unit,
    navigateToEnterSecondPassword: (CryptoAccountWithBalance) -> Unit,
    onBackPressed: () -> Unit
) {
    val viewState: SellSourceAccountsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(SellSourceAccountsIntent.LoadData)
        analytics.logEvent(SellAnalyticsEvents.SelectSourceViewed)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { navEvent ->
            when (navEvent) {
                is SellSourceAccountsNavigationEvent.ConfirmSelection -> {
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
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_sell),
            onCloseClick = onBackPressed
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        AccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = viewState.accountList,
            onAccountClick = {
                viewModel.onIntent(SellSourceAccountsIntent.AccountSelected(it.id))
            },
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}
