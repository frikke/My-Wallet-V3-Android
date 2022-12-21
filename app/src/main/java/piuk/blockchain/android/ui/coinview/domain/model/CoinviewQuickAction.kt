package piuk.blockchain.android.ui.coinview.domain.model

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
    object Buy : CoinviewQuickAction
    object Sell : CoinviewQuickAction
    object Send : CoinviewQuickAction
    object Receive : CoinviewQuickAction
    object Swap : CoinviewQuickAction
}
