package com.blockchain.home.presentation.quickactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun MoreActions(
    viewModel: QuickActionsViewModel = getViewModel(scope = payloadScope),
    assetActionsNavigation: AssetActionsNavigation,
    dismiss: () -> Unit
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: QuickActionsViewState? by stateFlowLifecycleAware.collectAsState(null)
    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(QuickActionsIntent.LoadActions(ActionType.More))
        onDispose { }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(
            title = stringResource(id = R.string.common_more),
            onClosePress = dismiss,
            startImageResource = ImageResource.None,
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        viewState?.moreActions?.forEachIndexed { index, item ->
            DefaultTableRow(
                modifier = Modifier.alpha(if (item.enabled) 1F else 0.5F),
                primaryText = stringResource(id = item.title),
                secondaryText = stringResource(id = item.subtitle),
                startImageResource = ImageResource.Local(item.icon),
                onClick = {
                    if (item.enabled) {
                        when (item.action.assetAction) {
                            AssetAction.Send -> assetActionsNavigation.navigate(item.action.assetAction)
                            AssetAction.FiatDeposit -> {
                                viewModel.onIntent(QuickActionsIntent.RunFiatAction(AssetAction.FiatDeposit))
                            }
                            AssetAction.FiatWithdraw -> {
                                viewModel.onIntent(QuickActionsIntent.RunFiatAction(AssetAction.FiatWithdraw))
                            }
                            else -> {
                                // n/a
                            }
                        }
                    }
                }
            )
            if (viewState?.moreActions?.lastIndex != index) {
                Divider()
            }
        }
        Spacer(modifier = Modifier.size(navBarHeight))
    }
}
