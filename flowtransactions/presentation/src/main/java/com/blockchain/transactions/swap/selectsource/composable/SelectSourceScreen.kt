package com.blockchain.transactions.swap.selectsource.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.setResult
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.accounts.composable.AccountList
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.transactions.swap.selectsource.SelectSourceIntent
import com.blockchain.transactions.swap.selectsource.SelectSourceNavigationEvent
import com.blockchain.transactions.swap.selectsource.SelectSourceViewModel
import com.blockchain.transactions.swap.selectsource.SelectSourceViewState
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectSourceScreen(
    viewModel: SelectSourceViewModel = getViewModel(scope = payloadScope),
    accountSelected: (CryptoAccountWithBalance) -> Unit,
    onBackPressed: () -> Unit
) {

    val viewState: SelectSourceViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(SelectSourceIntent.LoadData)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { navEvent ->
            when (navEvent) {
                is SelectSourceNavigationEvent.ConfirmSelection -> {
                    accountSelected(navEvent.account)
                    onBackPressed()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_swap_from),
            onCloseClick = onBackPressed
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        AccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = viewState.accountList,
            onAccountClick = {
                viewModel.onIntent(SelectSourceIntent.AccountSelected(it.id))
            },
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}
