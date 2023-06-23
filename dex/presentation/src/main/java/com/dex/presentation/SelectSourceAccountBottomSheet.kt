package com.dex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectSourceAccountBottomSheet(
    closeClicked: () -> Unit,
    viewModel: DexSourceAccountViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
) {
    val viewState: SourceAccountSelectionViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(SourceAccountIntent.LoadSourceAccounts)
        analytics.logEvent(DexAnalyticsEvents.SelectSourceOpened)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundMuted)
    ) {
        SheetFloatingHeader(
            icon = StackedIcon.None,
            title = stringResource(id = com.blockchain.stringResources.R.string.your_assets),
            onCloseClick = closeClicked
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            DexAccountSelection(
                accounts = viewState.accounts,
                warning = (viewState.inProgressTxStatus as? InProgressTxStatus.PendingTx)?.let {
                    {
                        TxInProgressWarning(
                            close = {
                                viewModel.onIntent(SourceAccountIntent.WarningDismissed)
                            }, network = it.coinNetwork.shortName
                        )
                    }
                },
                onAccountSelected = {
                    viewModel.onIntent(SourceAccountIntent.OnAccountSelected(it))
                    closeClicked()
                },
                onSearchTermUpdated = {
                    viewModel.onIntent(SourceAccountIntent.Search(it))
                }
            )
        }
    }
}

@Composable
private fun TxInProgressWarning(
    modifier: Modifier = Modifier,
    network: String,
    close: (() -> Unit) = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing),
                color = Color.White
            )
    ) {
        Row(Modifier.padding(all = AppTheme.dimensions.tinySpacing)) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier.clickable { close() },
                imageResource = Icons.Close.withTint(Grey400)
                    .withBackground(
                        backgroundColor = Grey000,
                        backgroundSize = AppTheme.dimensions.standardSpacing
                    )
            )
        }
        Column(Modifier.padding(all = AppTheme.dimensions.smallSpacing)) {
            SimpleText(
                text = stringResource(id = com.blockchain.stringResources.R.string.tx_in_process),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
            SimpleText(
                modifier = Modifier.padding(top = AppTheme.dimensions.minusculeSpacing),
                text = stringResource(id = com.blockchain.stringResources.R.string.tx_in_process_for_network, network),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    }
}
