package com.blockchain.betternavigation.example.swap

import androidx.navigation.NavGraphBuilder
import com.blockchain.betternavigation.Destination
import com.blockchain.betternavigation.DestinationWithArgs
import com.blockchain.betternavigation.NavGraph
import com.blockchain.betternavigation.navGraph
import com.blockchain.betternavigation.navigateTo
import com.blockchain.betternavigation.typedComposable

internal object SwapGraph : NavGraph() {
    object EnterAmount : Destination()
    object Confirmation : DestinationWithArgs<SwapConfirmationArgs>()
    object OrderStatus : DestinationWithArgs<SwapOrderStatusArgs>()
}

internal fun NavGraphBuilder.swapGraph() {
    navGraph(
        graph = SwapGraph,
        startDestination = SwapGraph.EnterAmount
    ) {
        typedComposable(SwapGraph.EnterAmount) {
            SwapEnterAmountScreen(
                navigateToConfirmation = { amount, source, target ->
                    val confirmationArgs = SwapConfirmationArgs(amount, source, target)
                    navigateTo(SwapGraph.Confirmation, confirmationArgs)
                }
            )
        }

        typedComposable(SwapGraph.Confirmation) { args ->
            SwapConfirmationScreen(
                args = args,
                navigateToOrderStatus = { order ->
                    val orderStatusArgs = SwapOrderStatusArgs(order)
                    navigateTo(SwapGraph.OrderStatus, orderStatusArgs)
                }
            )
        }

        typedComposable(SwapGraph.OrderStatus) { args ->
            SwapOrderStatusScreen(
                args = args
            )
        }
    }
}
