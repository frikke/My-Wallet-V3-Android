package com.blockchain.home.presentation.quickactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.blockchain.home.presentation.navigation.AssetActionsNavigation

@Composable
fun QuickActions(
    quickActionItems: List<QuickActionItem>,
    assetActionsNavigation: AssetActionsNavigation,
    quickActionsViewModel: QuickActionsViewModel,
    openMoreQuickActions: () -> Unit
) {
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
                    .clickableWithIndication {
                        if (quickAction.enabled)
                            when (quickAction.action) {
                                is QuickAction.TxAction -> when (quickAction.action.assetAction) {
                                    AssetAction.Send,
                                    AssetAction.Swap,
                                    AssetAction.Sell,
                                    AssetAction.Receive,
                                    AssetAction.Buy -> assetActionsNavigation.navigate(quickAction.action.assetAction)
                                    AssetAction.FiatDeposit -> quickActionsViewModel.onIntent(
                                        QuickActionsIntent.FiatAction(AssetAction.FiatDeposit)
                                    )
                                    AssetAction.FiatWithdraw -> quickActionsViewModel.onIntent(
                                        QuickActionsIntent.FiatAction(AssetAction.FiatWithdraw)
                                    )
                                    AssetAction.ViewActivity,
                                    AssetAction.ViewStatement,
                                    AssetAction.InterestDeposit,
                                    AssetAction.InterestWithdraw,
                                    AssetAction.Sign,
                                    AssetAction.StakingDeposit -> {
                                    }
                                }
                                is QuickAction.More -> openMoreQuickActions()
                            }
                        else {
                            // do nothing
                        }
                    }
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
