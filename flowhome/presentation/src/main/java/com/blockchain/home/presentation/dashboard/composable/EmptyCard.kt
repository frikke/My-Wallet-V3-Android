package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.walletmode.WalletMode

fun LazyListScope.emptyCard(
    walletMode: WalletMode,
    assetsViewState: AssetsViewState,
    actiityViewState: ActivityViewState,
    assetActionsNavigation: AssetActionsNavigation,
) {
    val state = dashboardState(assetsViewState, actiityViewState)

    if (state == DashboardState.EMPTY) {
        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            when (walletMode) {
                WalletMode.CUSTODIAL -> CustodialEmptyStateCards(
                    assetActionsNavigation = assetActionsNavigation
                )
                WalletMode.NON_CUSTODIAL -> NonCustodialEmptyStateCard {
                    assetActionsNavigation.navigate(AssetAction.Receive)
                }
            }
        }
    }
}

enum class DashboardState {
    EMPTY, NON_EMPTY, UNKNOWN
}

fun dashboardState(
    assetsViewState: AssetsViewState,
    activityViewState: ActivityViewState?,
): DashboardState {
    activityViewState ?: return DashboardState.UNKNOWN
    val hasAnyActivity =
        (activityViewState.activity as? DataResource.Data)?.data?.any { act -> act.value.isNotEmpty() }
            ?: return DashboardState.UNKNOWN
    val hasAnyAssets =
        (assetsViewState.assets as? DataResource.Data)?.data?.isNotEmpty() ?: return DashboardState.UNKNOWN

    return if (hasAnyActivity || hasAnyAssets) DashboardState.NON_EMPTY
    else DashboardState.EMPTY
}
