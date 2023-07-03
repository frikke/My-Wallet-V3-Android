package com.dex.presentation.inprogress

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.ScreenStatusIcon
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Pending
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import com.dex.presentation.DexAnalyticsEvents
import info.blockchain.balance.CryptoCurrency
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun DexInProgressTransaction(
    closeFlow: () -> Unit = {},
    retry: () -> Unit = {},
    viewModel: DexInProgressTxViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
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

    LaunchedEffect(viewState) {
        val event = when (viewState) {
            is InProgressViewState.Success -> DexAnalyticsEvents.ExecutedViewed
            InProgressViewState.Failure -> DexAnalyticsEvents.FailedViewed
            InProgressViewState.Loading -> DexAnalyticsEvents.InProgressViewed
        }
        analytics.logEvent(event)
    }

    DexInProgressTransactionScreen(
        viewState = viewState,
        retry = retry,
        closeFlow = closeFlow
    )
}

@Composable
private fun DexInProgressTransactionScreen(
    viewState: InProgressViewState,
    retry: () -> Unit,
    closeFlow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(all = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            modifier = Modifier
                .align(Alignment.End)
                .clickableNoEffect { closeFlow() },
            imageResource = Icons.Close.withTint(AppColors.body)
                .withBackground(
                    backgroundColor = AppColors.backgroundSecondary,
                    backgroundSize = AppTheme.dimensions.standardSpacing
                )
        )
        when (viewState) {
            is InProgressViewState.Success -> SuccessScreen(
                doneClicked = closeFlow,
                explorerUrl = viewState.txExplorerUrl,
                sourceCurrency = viewState.sourceCurrency.displayTicker,
                destinationCurrency = viewState.destinationCurrency.displayTicker
            )

            InProgressViewState.Failure -> FailureScreen(
                cancelClicked = closeFlow,
                tryAgain = retry
            )

            InProgressViewState.Loading -> {
            }
        }
    }
}

@Composable
private fun ColumnScope.FailureScreen(
    cancelClicked: () -> Unit = {},
    tryAgain: () -> Unit = {}
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ScreenStatusIcon(
            main = Icons.Swap,
            tag = Icons.Filled.Alert
                .withTint(AppColors.error),
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        SimpleText(
            text = stringResource(com.blockchain.stringResources.R.string.swap_failed),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))
        SimpleText(
            text = stringResource(
                com.blockchain.stringResources.R.string.swap_failed_message
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
        MinimalPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = com.blockchain.stringResources.R.string.common_cancel),
            onClick = cancelClicked
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = com.blockchain.stringResources.R.string.common_try_again),
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
        ScreenStatusIcon(
            main = Icons.Swap,
            tag = Icons.Filled.Pending
                .withTint(AppColors.muted),
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        SimpleText(
            text = stringResource(
                com.blockchain.stringResources.R.string.swapping_with_currencies,
                sourceCurrency,
                destinationCurrency
            ),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))
        SimpleText(
            text = stringResource(
                com.blockchain.stringResources.R.string.your_swap_is_confirmed,
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
        MinimalPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = com.blockchain.stringResources.R.string.view_on_explorer),
            onClick = {
                uriHandler.openUri(explorerUrl)
            }
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = com.blockchain.stringResources.R.string.common_done),
            onClick = doneClicked
        )
    }
}

@Preview
@Composable
private fun PreviewDexInProgressTransactionScreenFailure() {
    DexInProgressTransactionScreen(
        viewState = InProgressViewState.Failure,
        retry = {},
        closeFlow = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDexInProgressTransactionScreenFailureDark() {
    PreviewDexInProgressTransactionScreenFailure()
}

@Preview
@Composable
private fun PreviewDexInProgressTransactionScreen() {
    DexInProgressTransactionScreen(
        viewState = InProgressViewState.Success(
            sourceCurrency = CryptoCurrency.ETHER,
            destinationCurrency = CryptoCurrency.BTC,
            txExplorerUrl = ""
        ),
        retry = {},
        closeFlow = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDexInProgressTransactionScreenDark() {
    PreviewDexInProgressTransactionScreen()
}
