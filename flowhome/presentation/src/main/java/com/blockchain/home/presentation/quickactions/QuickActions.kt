package com.blockchain.home.presentation.quickactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Bank
import com.blockchain.componentlib.icons.Cash
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.MenuKebabHorizontal
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.clickableWithIndication
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.home.presentation.dashboard.composable.DashboardState
import com.blockchain.home.presentation.dashboard.eventName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get

val maxQuickActionsOnScreen: Int
    @Stable @Composable
    get() {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val horizontalSpacing = AppTheme.dimensions.smallSpacing
        val quickActionWidth = AppTheme.dimensions.xHugeSpacing
        val spaceBetween = AppTheme.dimensions.standardSpacing

        val availableScreenWidth = (screenWidth + spaceBetween - (horizontalSpacing * 2))

        return (availableScreenWidth / (quickActionWidth + spaceBetween)).toInt()
    }

@Composable
fun QuickActions(
    analytics: Analytics = get(),
    quickActionItems: ImmutableList<QuickActionItem>,
    dashboardState: DashboardState,
    quickActionClicked: (QuickActionItem) -> Unit
) {
    QuickActionsScreen(
        analytics = analytics,
        quickActionItems = quickActionItems,
        dashboardState = dashboardState,
        onActionClicked = quickActionClicked
    )
}

@Composable
private fun QuickActionsScreen(
    analytics: Analytics = get(),
    quickActionItems: ImmutableList<QuickActionItem>,
    dashboardState: DashboardState,
    onActionClicked: (QuickActionItem) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        quickActionItems.forEachIndexed { index, quickAction ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .then(
                        if (quickAction.enabled) {
                            Modifier.clickableWithIndication {
                                onActionClicked(quickAction)
                                (quickAction.action as? QuickAction.TxAction)?.assetAction?.let { assetAction ->
                                    assetAction.eventName()?.let {
                                        analytics.logEvent(
                                            DashboardAnalyticsEvents.QuickActionClicked(
                                                assetAction = assetAction,
                                                state = dashboardState
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Image(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (quickAction.enabled) 1f else .6f
                        },
                    imageResource = quickAction.icon
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                Text(
                    text = stringResource(id = quickAction.title),
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.body,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (quickAction.enabled) 1f else .6f
                        }
                )
            }

            if (index < quickActionItems.lastIndex) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
        }
    }
}

private val QuickActionItem.icon: ImageResource
    @Composable
    get() {
        val icon = when (action) {
            is QuickAction.TxAction -> when (action.assetAction) {
                AssetAction.Send -> Icons.Send
                AssetAction.Swap -> Icons.Swap
                AssetAction.Sell -> Icons.Minus
                AssetAction.FiatDeposit -> Icons.Bank
                AssetAction.FiatWithdraw -> Icons.Cash
                AssetAction.Buy -> Icons.Plus
                AssetAction.Receive -> Icons.Receive
                else -> throw UnsupportedOperationException()
            }

            QuickAction.More -> Icons.MenuKebabHorizontal
        }

        return icon
            .withTint(AppTheme.colors.title)
            .withBackground(
                backgroundColor = AppTheme.colors.backgroundSecondary,
                iconSize = AppTheme.dimensions.standardSpacing,
                backgroundSize = AppTheme.dimensions.xHugeSpacing
            )
    }

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
fun PreviewQuickActionsScreen() {
    QuickActionsScreen(
        analytics = previewAnalytics,
        quickActionItems = listOf(
            QuickActionItem(
                title = com.blockchain.stringResources.R.string.common_send,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            QuickActionItem(
                title = com.blockchain.stringResources.R.string.common_send,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            QuickActionItem(
                title = com.blockchain.stringResources.R.string.common_send,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            QuickActionItem(
                title = com.blockchain.stringResources.R.string.common_more,
                action = QuickAction.More,
                enabled = true
            )
        ).toImmutableList(),
        dashboardState = DashboardState.NON_EMPTY,
        onActionClicked = {}
    )
}
