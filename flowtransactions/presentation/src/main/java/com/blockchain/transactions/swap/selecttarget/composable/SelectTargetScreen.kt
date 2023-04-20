package com.blockchain.transactions.swap.selecttarget.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.common.prices.composable.SelectAssetPricesList
import com.blockchain.transactions.presentation.R
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

        SelectAssetPricesList(
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
            assets = viewState.prices,
            onAccountClick = {

            },
            bottomSpacer = AppTheme.dimensions.smallSpacing
        )
    }
}
