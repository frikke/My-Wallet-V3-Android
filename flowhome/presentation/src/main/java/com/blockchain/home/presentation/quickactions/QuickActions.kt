package com.blockchain.home.presentation.quickactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.MenuKebabHorizontal
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun QuickActions(
    viewModel: QuickActionsViewModel = getViewModel(scope = payloadScope),
    forceRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    openMoreQuickActions: () -> Unit,
) {
    val viewState: QuickActionsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(QuickActionsIntent.LoadActions(ActionType.Quick))
        onDispose { }
    }
    DisposableEffect(key1 = forceRefresh) {
        if (forceRefresh) {
            viewModel.onIntent(QuickActionsIntent.Refresh)
        }
        onDispose { }
    }

    QuickActionsRow(
        quickActionItems = viewState.actions,
        onClick = { action ->
            when (action) {
                is QuickAction.TxAction -> assetActionsNavigation.navigate(action.assetAction)
                is QuickAction.More -> openMoreQuickActions()
            }
        }
    )
}

@Composable
fun QuickActionsRow(quickActionItems: List<QuickActionItem>, onClick: (QuickAction) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        quickActionItems.forEach { quickAction ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = AppTheme.dimensions.verySmallSpacing)
                    .clickable(onClick = {
                        if (quickAction.enabled)
                            onClick(quickAction.action)
                        else {
                            // do nothing
                        }
                    })
            ) {
                Image(
                    modifier = Modifier
                        .padding(top = 2.dp, bottom = 2.dp)
                        .alpha(
                            if (quickAction.enabled) 1f else .6f
                        ),
                    imageResource = quickAction.icon
                )
                Text(
                    text = stringResource(id = quickAction.title),
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .alpha(
                            if (quickAction.enabled) 1f else .6f
                        )
                )
            }
        }
    }
}

private val QuickActionItem.icon: ImageResource
    @Composable
    get() = when (action) {
        is QuickAction.TxAction -> when (action.assetAction) {
            AssetAction.Send -> Icons.Send
            AssetAction.Swap -> Icons.Swap
            AssetAction.Sell -> Icons.Minus
            AssetAction.Buy -> Icons.Plus
            AssetAction.Receive -> Icons.Receive
            else -> throw UnsupportedOperationException()
        }
        QuickAction.More -> Icons.MenuKebabHorizontal
    }.withBackground(
        backgroundColor = White,
        iconSize = AppTheme.dimensions.standardSpacing,
        backgroundSize = AppTheme.dimensions.xHugeSpacing,
    )

@Preview
@Composable
fun QuickActionsPreview() {
    AppTheme {
        AppSurface {
            QuickActionsRow(
                listOf(
                    QuickActionItem(
                        title = R.string.common_buy,
                        action = QuickAction.TxAction(AssetAction.Swap),
                        enabled = false
                    ),
                    QuickActionItem(
                        title = R.string.common_buy,
                        enabled = true,
                        action = QuickAction.TxAction(AssetAction.Swap)
                    ),
                    QuickActionItem(
                        title = R.string.common_buy,
                        enabled = false,
                        action = QuickAction.TxAction(AssetAction.Swap)
                    )
                ),
                {}
            )
        }
    }
}
