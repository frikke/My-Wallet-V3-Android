package com.blockchain.home.presentation.quickactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.theme.clickableWithIndication
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.home.presentation.dashboard.actionName
import com.blockchain.home.presentation.dashboard.composable.DashboardState
import com.blockchain.home.presentation.dashboard.composable.dashboardState
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import org.koin.androidx.compose.get

val maxQuickActionsOnScreen: Int
    @Stable @Composable get() {
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
    quickActionItems: List<QuickActionItem>,
    assetActionsNavigation: AssetActionsNavigation,
    quickActionsViewModel: QuickActionsViewModel,
    dashboardState: DashboardState,
    openMoreQuickActions: () -> Unit
) {
    QuickActionsScreen(
        analytics = analytics,
        quickActionItems = quickActionItems,
        dashboardState = dashboardState,
        buyOnClick = { action ->
            assetActionsNavigation.navigate(action)
        },
        fiatDepositOnClick = {
            quickActionsViewModel.onIntent(
                QuickActionsIntent.FiatAction(AssetAction.FiatDeposit)
            )
        },
        fiatWithdrawOnClick = {
            quickActionsViewModel.onIntent(
                QuickActionsIntent.FiatAction(AssetAction.FiatWithdraw)
            )
        },
        moreOnClick = openMoreQuickActions,
    )
}

@Composable
private fun QuickActionsScreen(
    analytics: Analytics = get(),
    quickActionItems: List<QuickActionItem>,
    dashboardState: DashboardState,
    buyOnClick: (AssetAction) -> Unit,
    fiatDepositOnClick: () -> Unit,
    fiatWithdrawOnClick: () -> Unit,
    moreOnClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        quickActionItems.forEachIndexed { index, quickAction ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .then(
                        if (quickAction.enabled) {
                            Modifier.clickableWithIndication {
                                when (quickAction.action) {
                                    is QuickAction.TxAction -> when (val action = quickAction.action.assetAction) {
                                        AssetAction.Send,
                                        AssetAction.Swap,
                                        AssetAction.Sell,
                                        AssetAction.Receive,
                                        AssetAction.Buy -> buyOnClick(action)
                                        AssetAction.FiatDeposit -> fiatDepositOnClick()
                                        AssetAction.FiatWithdraw -> fiatWithdrawOnClick()
                                        AssetAction.ViewActivity,
                                        AssetAction.ViewStatement,
                                        AssetAction.InterestDeposit,
                                        AssetAction.InterestWithdraw,
                                        AssetAction.Sign,
                                        AssetAction.StakingDeposit -> {
                                        }
                                    }
                                    is QuickAction.More -> moreOnClick()
                                }

                                (quickAction.action as? QuickAction.TxAction)?.assetAction?.let { assetAction ->
                                    assetAction
                                        .actionName()
                                        ?.let {
                                            analytics.logEvent(
                                                DashboardAnalyticsEvents.QuickActionClicked(
                                                    actionName = it,
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

            if (index < quickActionItems.lastIndex) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
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
            AssetAction.FiatDeposit -> Icons.Bank
            AssetAction.FiatWithdraw -> Icons.Cash
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

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
fun PreviewQuickActionsScreen() {
    QuickActionsScreen(
        analytics = previewAnalytics,
        quickActionItems = listOf(
            QuickActionItem(
                title = R.string.common_send,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            QuickActionItem(
                title = R.string.common_send,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            QuickActionItem(
                title = R.string.common_send,
                action = QuickAction.TxAction(AssetAction.Send),
                enabled = true
            ),
            QuickActionItem(
                title = R.string.common_more,
                action = QuickAction.More,
                enabled = true
            )
        ),
        dashboardState = DashboardState.NON_EMPTY,
        buyOnClick = {}, fiatDepositOnClick = {}, fiatWithdrawOnClick = {}, moreOnClick = {}
    )
}
