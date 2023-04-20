package com.blockchain.transactions.swap.selecttarget.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
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
import com.blockchain.transactions.swap.selectsource.composable.KEY_SWAP_SOURCE_ACCOUNT
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.blockchain.transactions.swap.selecttarget.SelectTargetIntent
import com.blockchain.transactions.swap.selecttarget.SelectTargetViewModel
import com.blockchain.transactions.swap.selecttarget.SelectTargetViewState
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

const val KEY_SWAP_TARGET_ACCOUNT = "KEY_SWAP_TARGET_ACCOUNT"

@Composable
fun SelectTargetScreen(
    sourceTicker: String,
    viewModel: SelectTargetViewModel = getViewModel(
        scope = payloadScope,
        key = sourceTicker,
        parameters = { parametersOf(sourceTicker) }
    ),
    navControllerProvider: () -> NavHostController,
    onBackPressed: () -> Unit
) {

    val viewState: SelectTargetViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(SelectTargetIntent.LoadData)
        onDispose { }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_swap_to),
            onCloseClick = onBackPressed
        )

        StandardVerticalSpacer()

        //        AccountList(
        //            modifier = Modifier.padding(
        //                horizontal = AppTheme.dimensions.smallSpacing
        //            ),
        //            accounts = viewState.accountList,
        //            onAccountClick = {
        //                navControllerProvider().setResult(KEY_SWAP_TARGET_ACCOUNT, it.ticker)
        //                onBackPressed()
        //            },
        //            bottomSpacer = AppTheme.dimensions.smallSpacing
        //        )
    }
}
