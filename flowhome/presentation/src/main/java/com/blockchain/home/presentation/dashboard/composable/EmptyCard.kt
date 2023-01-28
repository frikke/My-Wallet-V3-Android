package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.walletmode.WalletMode

fun LazyListScope.emptyCard(
    wMode: WalletMode?,
    assetsViewState: AssetsViewState,
    aViewState: ActivityViewState?,
    assetActionsNavigation: AssetActionsNavigation,
) {
    val walletMode = wMode ?: return
    val activityViewState = aViewState ?: return
    val hasAnyActivity =
        (activityViewState.activity as? DataResource.Data)?.data?.any { act -> act.value.isNotEmpty() } ?: return
    val hasAnyAssets = (assetsViewState.assets as? DataResource.Data)?.data?.isNotEmpty() ?: return

    if (hasAnyActivity.not() && hasAnyAssets.not()) {
        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
            when (walletMode) {
                WalletMode.CUSTODIAL -> CustodialEmptyStateCards(
                    assetActionsNavigation
                )
                WalletMode.NON_CUSTODIAL -> NonCustodialEmptyStateCard {
                    assetActionsNavigation.navigate(AssetAction.Receive)
                }
            }
        }
    }
}
