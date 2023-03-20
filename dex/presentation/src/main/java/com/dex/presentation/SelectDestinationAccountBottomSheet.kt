package com.dex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectDestinationAccountBottomSheet(
    closeClicked: () -> Unit,
    viewModel: DexSelectDestinationAccountViewModel = getViewModel(scope = payloadScope)
) {

    val viewState: DestinationAccountSelectionViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(DestinationAccountIntent.LoadAccounts)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundMuted)
    ) {
        SheetFloatingHeader(
            icon =
            StackedIcon.None,
            title = stringResource(id = R.string.select_token),
            onCloseClick = closeClicked
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            DexAccountSelection(
                accounts = viewState.accounts,
                onAccountSelected = {
                    viewModel.onIntent(DestinationAccountIntent.OnAccountSelected(it))
                    closeClicked()
                },
                onSearchTermUpdated = {
                    viewModel.onIntent(DestinationAccountIntent.Search(it))
                }
            )
        }
    }
}
