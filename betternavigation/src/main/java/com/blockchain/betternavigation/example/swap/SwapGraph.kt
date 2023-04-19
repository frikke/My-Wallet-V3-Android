package com.blockchain.betternavigation.example.swap

import androidx.navigation.NavGraphBuilder
import com.blockchain.betternavigation.BetterDestination
import com.blockchain.betternavigation.BetterDestinationWithArgs
import com.blockchain.betternavigation.BetterNavGraph
import com.blockchain.betternavigation.betterDestination
import com.blockchain.betternavigation.betterNavGraph
import com.blockchain.betternavigation.navigateTo

internal object SwapGraph : BetterNavGraph() {
    object EnterAmount : BetterDestination()
    object Confirmation : BetterDestinationWithArgs<SwapConfirmationArgs>()
    object OrderStatus : BetterDestinationWithArgs<SwapOrderStatusArgs>()
}

internal fun NavGraphBuilder.swapGraph() {
    betterNavGraph(
        graph = SwapGraph,
        startDestination = SwapGraph.EnterAmount,
    ) {
        betterDestination(SwapGraph.EnterAmount) {
            SwapEnterAmountScreen(
                navigateToConfirmation = { amount, source, target ->
                    val confirmationArgs = SwapConfirmationArgs(amount, source, target)
                    navigateTo(SwapGraph.Confirmation, confirmationArgs)
                }
            )
        }

        betterDestination(SwapGraph.Confirmation) { args ->
            SwapConfirmationScreen(
                args = args,
                navigateToOrderStatus = { order ->
                    val orderStatusArgs = SwapOrderStatusArgs(order)
                    navigateTo(SwapGraph.OrderStatus, orderStatusArgs)
                }
            )
        }

        betterDestination(SwapGraph.OrderStatus) { args ->
            SwapOrderStatusScreen(
                args = args,
            )
        }
    }
}
