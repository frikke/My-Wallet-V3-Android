package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.allassets.AssetsViewState

fun LazyListScope.defiEmptyCard(
    assetsViewState: AssetsViewState,
    onReceiveClicked: () -> Unit
) {
    val state = dashboardState(assetsViewState)

    if (state == DashboardState.EMPTY) {
        paddedItem(
            paddingValues = {
                PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
            }
        ) {
            NonCustodialEmptyStateCard(
                onReceiveClicked = onReceiveClicked
            )
        }
    }
}

enum class DashboardState {
    EMPTY, NON_EMPTY, UNKNOWN
}

fun dashboardState(
    assetsViewState: AssetsViewState,
): DashboardState {
    val shouldShowEmptyStateForAssets = assetsViewState.showNoResults
    return if (shouldShowEmptyStateForAssets) {
        DashboardState.EMPTY
    } else DashboardState.NON_EMPTY
}
