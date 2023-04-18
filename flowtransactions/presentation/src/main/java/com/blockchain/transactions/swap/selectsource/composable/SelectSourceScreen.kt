package com.blockchain.transactions.swap.selectsource.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.composable.AccountList
import com.blockchain.transactions.swap.selectsource.SelectSourceIntent
import com.blockchain.transactions.swap.selectsource.SelectSourceViewModel
import com.blockchain.transactions.swap.selectsource.SelectSourceViewState
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectSourceScreen(
    viewModel: SelectSourceViewModel = getViewModel(scope = payloadScope)
) {

    val viewState: SelectSourceViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(SelectSourceIntent.LoadData)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {

        SheetHeader(onClosePress = { /*TODO*/ }, title = "Swap from")

        StandardVerticalSpacer()

        AccountList(accounts = viewState.accountList, onAccountClick = {})
    }
}

@Preview
@Composable
fun SelectSourceScreenPreview() {
    AppTheme {
        SelectSourceScreen()
    }
}
