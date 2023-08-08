package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.domain.swap.SwapOption

data class CoinviewQuickActions(
    val center: List<CoinviewQuickAction>,
    val bottom: List<CoinviewQuickAction>
) {
    val actions: List<CoinviewQuickAction>
        get() = center + bottom

    companion object {
        fun none() = CoinviewQuickActions(
            center = listOf(),
            bottom = listOf()
        )
    }
}

sealed interface CoinviewQuickAction {
    val enabled: Boolean

    data class Buy(override val enabled: Boolean = true) : CoinviewQuickAction
    data class Sell(override val enabled: Boolean = true) : CoinviewQuickAction
    data class Send(override val enabled: Boolean = true) : CoinviewQuickAction
    data class Receive(override val enabled: Boolean = true) : CoinviewQuickAction
    data class Swap(override val enabled: Boolean = true, val swapOption: SwapOption) : CoinviewQuickAction
    data class Get(override val enabled: Boolean = true, val swapOption: SwapOption) : CoinviewQuickAction
}
