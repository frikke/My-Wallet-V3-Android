package com.blockchain.home.presentation.quickactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStoreOwner
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.home.presentation.R
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun MoreActions(
    viewModel: QuickActionsViewModel = getViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
        scope = payloadScope
    ),
    assetActionsNavigation: AssetActionsNavigation,
    dismiss: () -> Unit
) {
    val viewState: QuickActionsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    MoreActionsScreen(
        actions = viewState.moreActions,
        onActionClick = { action ->
            when (action) {
                AssetAction.Send,
                AssetAction.Sell,
                AssetAction.Receive,
                AssetAction.Buy,
                AssetAction.Swap -> assetActionsNavigation.navigate(action)
                AssetAction.FiatDeposit -> {
                    viewModel.onIntent(QuickActionsIntent.FiatAction(AssetAction.FiatDeposit))
                }
                AssetAction.FiatWithdraw -> {
                    viewModel.onIntent(QuickActionsIntent.FiatAction(AssetAction.FiatWithdraw))
                }
                else -> {
                    // n/a
                }
            }
        },
        dismiss = dismiss
    )
}

@Composable
private fun MoreActionsScreen(
    actions: List<MoreActionItem>,
    onActionClick: (AssetAction) -> Unit,
    dismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(AppTheme.colors.backgroundSecondary)) {
        SheetHeader(
            title = stringResource(id = com.blockchain.stringResources.R.string.common_more),
            onClosePress = dismiss,
            startImageResource = ImageResource.None,
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        actions.forEach { item ->
            DefaultTableRow(
                modifier = Modifier.alpha(if (item.enabled) 1F else 0.5F),
                primaryText = stringResource(id = item.title),
                secondaryText = stringResource(id = item.subtitle),
                startImageResource = ImageResource.Local(item.icon),
                endImageResource = if (item.enabled) {
                    ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_chevron_end)
                } else {
                    ImageResource.None
                },
                onClick = {
                    if (item.enabled) {
                        onActionClick(item.action.assetAction)
                    }
                }
            )

            if (actions.last() != item) {
                Divider(color = AppTheme.colors.background)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMoreActionsScreen() {
    MoreActionsScreen(
        actions = listOf(
            MoreActionItem(
                icon = R.drawable.ic_more_send,
                title = com.blockchain.stringResources.R.string.common_send,
                subtitle = com.blockchain.stringResources.R.string.transfer_to_other_wallets,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            MoreActionItem(
                icon = R.drawable.ic_more_send,
                title = com.blockchain.stringResources.R.string.common_send,
                subtitle = com.blockchain.stringResources.R.string.transfer_to_other_wallets,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            MoreActionItem(
                icon = R.drawable.ic_more_send,
                title = com.blockchain.stringResources.R.string.common_send,
                subtitle = com.blockchain.stringResources.R.string.transfer_to_other_wallets,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = false
            )
        ),
        onActionClick = {},
        dismiss = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPreviewMoreActionsScreenDark() {
    PreviewMoreActionsScreen()
}
