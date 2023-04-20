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
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.composable.AccountList
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.selectsource.SelectSourceIntent
import com.blockchain.transactions.swap.selectsource.SelectSourceViewModel
import com.blockchain.transactions.swap.selectsource.SelectSourceViewState
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen
import com.blockchain.transactions.swap.selecttarget.SelectTargetIntent
import com.blockchain.transactions.swap.selecttarget.SelectTargetViewModel
import com.blockchain.transactions.swap.selecttarget.SelectTargetViewState
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectTargetScreen(
    viewModel: SelectTargetViewModel = getViewModel(scope = payloadScope),
    onAccountSelected: (ticker: String) -> Unit,
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
            title = stringResource(R.string.common_swap_from),
            onCloseClick = onBackPressed
        )

        StandardVerticalSpacer()

//        AccountList(
//            modifier = Modifier.padding(
//                horizontal = AppTheme.dimensions.smallSpacing
//            ),
//            accounts = viewState.accountList,
//            onAccountClick = {
//                onAccountSelected(it.ticker)
//            },
//            bottomSpacer = AppTheme.dimensions.smallSpacing
//        )
    }
}