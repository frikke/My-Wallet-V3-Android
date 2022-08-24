package piuk.blockchain.android.ui.coinview.domain.model

data class CoinviewQuickActions(
    val center: CoinviewQuickAction,
    val bottomStart: CoinviewQuickAction,
    val bottomEnd: CoinviewQuickAction,
)

sealed interface CoinviewQuickAction {
    val enabled: Boolean

    data class Buy(override val enabled: Boolean) : CoinviewQuickAction
    data class Sell(override val enabled: Boolean) : CoinviewQuickAction
    data class Send(override val enabled: Boolean) : CoinviewQuickAction
    data class Receive(override val enabled: Boolean) : CoinviewQuickAction
    data class Swap(override val enabled: Boolean) : CoinviewQuickAction
    object None : CoinviewQuickAction {
        override val enabled = error("None action doesn't have enabled property")
    }
}
