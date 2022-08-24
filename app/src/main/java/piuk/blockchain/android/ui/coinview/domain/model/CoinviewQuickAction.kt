package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.BlockchainAccount

data class QuickActionData(
    val middleAction: CoinviewQuickAction,
    val startAction: CoinviewQuickAction,
    val endAction: CoinviewQuickAction,
)

sealed class CoinviewQuickAction(open val enabled: Boolean) {
    data class Buy(override val enabled: Boolean) : CoinviewQuickAction(enabled)
    data class Sell(override val enabled: Boolean) : CoinviewQuickAction(enabled)
    data class Send(override val enabled: Boolean) : CoinviewQuickAction(enabled)
    data class Receive(override val enabled: Boolean) : CoinviewQuickAction(enabled)
    data class Swap(override val enabled: Boolean) : CoinviewQuickAction(enabled)
    object None : CoinviewQuickAction(false)
}
