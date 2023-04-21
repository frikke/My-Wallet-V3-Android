package com.blockchain.betternavigation.example.swap

import androidx.compose.runtime.Composable
import java.io.Serializable

internal data class SwapConfirmationArgs(
    val amount: String,
    val source: String,
    val target: String,
) : Serializable

@Composable
internal fun SwapConfirmationScreen(
    args: SwapConfirmationArgs,
    viewModel: SwapConfirmationViewModel = getViewModel(args),
    navigateToOrderStatus: (Order) -> Unit,
) {
}

internal fun getViewModel(args: SwapConfirmationArgs): SwapConfirmationViewModel {
    return SwapConfirmationViewModel(args)
}

internal class SwapConfirmationViewModel(
    args: SwapConfirmationArgs
)
