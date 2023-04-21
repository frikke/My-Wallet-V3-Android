package com.blockchain.transactions.swap.selectsource.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.blockchain.transactions.swap.selectsource.SelectSourceIntent
import com.blockchain.transactions.swap.selectsource.SelectSourceViewModel
import com.blockchain.transactions.swap.selectsource.SelectSourceViewState
import org.koin.androidx.compose.getViewModel

const val KEY_SWAP_SOURCE_ACCOUNT = "KEY_SWAP_SOURCE_ACCOUNT"

@Composable
fun SelectSourceScreen(
    viewModel: SelectSourceViewModel = getViewModel(scope = payloadScope),
    navControllerProvider: () -> NavController,
    onBackPressed: () -> Unit
) {

    val viewState: SelectSourceViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(SelectSourceIntent.LoadData)
        onDispose { }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_swap_from),
            onCloseClick = onBackPressed
        )

        StandardVerticalSpacer()

        AccountList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            accounts = viewState.accountList,
            onAccountClick = {
                navControllerProvider().setResult(KEY_SWAP_SOURCE_ACCOUNT, it.ticker)
                onBackPressed()
            },
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}

@Preview
@Composable
private fun SelectSourceScreenPreview() {
    AppTheme {
        val nc = rememberNavController()
        SelectSourceScreen(
            navControllerProvider = { nc },
            onBackPressed = {}
        )
    }
}
