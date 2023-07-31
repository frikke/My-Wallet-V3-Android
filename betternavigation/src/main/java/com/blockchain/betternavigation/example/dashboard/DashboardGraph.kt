package com.blockchain.betternavigation.example.dashboard

import androidx.navigation.NavGraphBuilder
import com.blockchain.betternavigation.Destination
import com.blockchain.betternavigation.DestinationWithArgs
import com.blockchain.betternavigation.NavContext
import com.blockchain.betternavigation.NavGraph
import com.blockchain.betternavigation.navGraph
import com.blockchain.betternavigation.navigateTo
import com.blockchain.betternavigation.typedComposable

internal object DashboardGraph : NavGraph() {
    object Overview : Destination()
    object CoinView : DestinationWithArgs<DashboardCoinViewArgs>()
}

internal fun NavGraphBuilder.dashboardGraph(
    // When this pattern of delegating navigation between graph to the parent, BetterNavigationContext
    // needs to be passed as a receiver so the parent has access to navigateTo(Destination) methods
    navigateToSwap: NavContext.() -> Unit
) {
    navGraph(
        graph = DashboardGraph,
        startDestination = DashboardGraph.Overview
    ) {
        typedComposable(DashboardGraph.Overview) {
            DashboardOverviewScreen(
                navigateToCoinView = { asset ->
                    val coinViewArgs = DashboardCoinViewArgs(asset)
                    navigateTo(DashboardGraph.CoinView, coinViewArgs)
                }
            )
        }

        typedComposable(DashboardGraph.CoinView) { args ->
            DashboardCoinViewScreen(
                args = args,
                navigateToSwap = {
                    navigateToSwap()
                }
            )
        }
    }
}
