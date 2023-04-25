package com.blockchain.betternavigation.example

import androidx.compose.runtime.Composable
import com.blockchain.betternavigation.TypedNavHost
import com.blockchain.betternavigation.example.dashboard.DashboardGraph
import com.blockchain.betternavigation.example.dashboard.dashboardGraph
import com.blockchain.betternavigation.example.swap.SwapGraph
import com.blockchain.betternavigation.example.swap.swapGraph
import com.blockchain.betternavigation.navigateTo

@Composable
internal fun BetterNavigationExampleScreen() {
    TypedNavHost(
        startDestination = DashboardGraph,
    ) {
        dashboardGraph(
            navigateToSwap = {
                navigateTo(SwapGraph)
            }
        )

        swapGraph()
    }
}
