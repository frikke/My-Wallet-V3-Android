package com.dex.presentation.inprogress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Preview
@Composable
fun DexInProgressTransactionScreen(
    closeFlow: () -> Unit = {},
    viewModel: DexInProgressTxViewModel = getViewModel(scope = payloadScope),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(InProgressIntent.LoadTransactionProgress)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val viewState: InProgressViewState by viewModel.viewState.collectAsStateLifecycleAware()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (val state = viewState) {
            is InProgressViewState.Success -> SuccessScreen(
                doneClicked = closeFlow,
                explorerUrl = state.txExplorerUrl,
                sourceCurrency = state.sourceCurrency.displayTicker,
                destinationCurrency = state.destinationCurrency.displayTicker
            )
            InProgressViewState.Failure -> FailureScreen(
                cancelClicked = closeFlow,
                tryAgain = closeFlow // todo --> add retry
            )
            InProgressViewState.Loading -> {
            }
        }
    }
}

@Composable
private fun ColumnScope.FailureScreen(
    cancelClicked: () -> Unit,
    tryAgain: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            imageResource = ImageResource.Local(R.drawable.dex_transaction_failed)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        SimpleText(
            text = stringResource(R.string.swap_failed),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))
        SimpleText(
            text = stringResource(
                R.string.swap_failed_message,
            ),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
    Column(
        modifier = Modifier
            .align(Alignment.End)
    ) {
        MinimalButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.common_cancel),
            onClick = cancelClicked
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.common_try_again),
            onClick = tryAgain
        )
    }
}

@Composable
private fun ColumnScope.SuccessScreen(
    doneClicked: () -> Unit,
    explorerUrl: String,
    sourceCurrency: String,
    destinationCurrency: String
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            imageResource = ImageResource.Local(R.drawable.dex_transaction_completed)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        SimpleText(
            text = stringResource(R.string.swapping_with_currencies, sourceCurrency, destinationCurrency),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))
        SimpleText(
            text = stringResource(
                R.string.your_swap_is_confirmed,
                sourceCurrency,
                destinationCurrency
            ),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
    Column(
        modifier = Modifier
            .align(Alignment.End)
    ) {
        MinimalButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.view_on_explorer),
            onClick = {
                uriHandler.openUri(explorerUrl)
            }
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.common_done),
            onClick = doneClicked
        )
    }
}
