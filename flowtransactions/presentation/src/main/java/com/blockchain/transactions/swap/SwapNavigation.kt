package com.blockchain.transactions.swap

import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.transactions.swap.enteramount.composable.EnterAmount
import com.blockchain.transactions.swap.selectsource.composable.SelectSourceScreen

fun NavGraphBuilder.swapGraph(
    onBackPressed: () -> Unit
) {
    navigation(startDestination = SwapDestination.EnterAmount.route, route = SwapDestination.Main.route) {
        composable(navigationEvent = SwapDestination.EnterAmount) {
            ChromeSingleScreen {
                EnterAmount(
                    onBackPressed = onBackPressed
                )
//                SelectSourceScreen()
            }
        }
    }
}

sealed class SwapDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Main : SwapDestination("SwapMain")
    object EnterAmount : SwapDestination("SwapEnterAmount")
}
