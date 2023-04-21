package com.blockchain.betternavigation.example.dashboard

import androidx.navigation.NavGraphBuilder
import com.blockchain.betternavigation.BetterDestination
import com.blockchain.betternavigation.BetterDestinationWithArgs
import com.blockchain.betternavigation.BetterNavGraph
import com.blockchain.betternavigation.BetterNavigationContext
import com.blockchain.betternavigation.betterDestination
import com.blockchain.betternavigation.betterNavGraph
import com.blockchain.betternavigation.navigateTo

internal object DashboardGraph : BetterNavGraph() {
    object Overview : BetterDestination()
    object CoinView : BetterDestinationWithArgs<DashboardCoinViewArgs>()
}

internal fun NavGraphBuilder.dashboardGraph(
    // When this pattern of delegating navigation between graph to the parent, BetterNavigationContext
    // needs to be passed as a receiver so the parent has access to navigateTo(Destination) methods
    navigateToSwap: BetterNavigationContext.() -> Unit,
) {
    betterNavGraph(
        graph = DashboardGraph,
        startDestination = DashboardGraph.Overview,
    ) {
        betterDestination(DashboardGraph.Overview) {
            DashboardOverviewScreen(
                navigateToCoinView = { asset ->
                    val coinViewArgs = DashboardCoinViewArgs(asset)
                    navigateTo(DashboardGraph.CoinView, coinViewArgs)
                }
            )
        }

        betterDestination(DashboardGraph.CoinView) { args ->
            DashboardCoinViewScreen(
                args = args,
                navigateToSwap = {
                    navigateToSwap()
                }
            )
        }
    }
}
