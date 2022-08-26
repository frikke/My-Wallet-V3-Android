package piuk.blockchain.android.ui.coinview.domain.model

data class CoinviewQuickActions(
    val center: CoinviewQuickAction,
    val bottomStart: CoinviewQuickAction,
    val bottomEnd: CoinviewQuickAction,
) {
    companion object {
        fun none() = CoinviewQuickActions(
            center = CoinviewQuickAction.None,
            bottomStart = CoinviewQuickAction.None,
            bottomEnd = CoinviewQuickAction.None
        )
    }
}

sealed interface CoinviewQuickAction {
    val enabled: Boolean

    data class Buy(override val enabled: Boolean) : CoinviewQuickAction
    data class Sell(override val enabled: Boolean) : CoinviewQuickAction
    data class Send(override val enabled: Boolean) : CoinviewQuickAction
    data class Receive(override val enabled: Boolean) : CoinviewQuickAction
    data class Swap(override val enabled: Boolean) : CoinviewQuickAction
    object None : CoinviewQuickAction {
        override val enabled : Boolean get() = error("None action doesn't have enabled property")
    }
}
