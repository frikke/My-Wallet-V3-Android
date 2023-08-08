package com.blockchain.betternavigation.example.dashboard

import androidx.compose.runtime.Composable
import java.io.Serializable

internal data class DashboardCoinViewArgs(
    val asset: String
) : Serializable

@Composable
internal fun DashboardCoinViewScreen(
    args: DashboardCoinViewArgs,
    viewModel: DashboardCoinViewViewModel = getViewModel(args),
    navigateToSwap: () -> Unit
) {
}

internal fun getViewModel(args: DashboardCoinViewArgs): DashboardCoinViewViewModel {
    return DashboardCoinViewViewModel(args)
}

internal class DashboardCoinViewViewModel(
    args: DashboardCoinViewArgs
)
