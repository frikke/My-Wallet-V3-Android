package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount

sealed interface CoinviewNavigationEvent : NavigationEvent {
    data class ShowAccountExplainer(
        val cvAccount: CoinviewAccount,
        val networkTicker: String,
        val interestRate: Double,
        val actions: List<StateAwareAction>
    ) : CoinviewNavigationEvent

    data class NavigateToBuy(
        val asset: CryptoAsset,
    ) : CoinviewNavigationEvent

    data class NavigateToSell(
        val cvAccount: CoinviewAccount
    ) : CoinviewNavigationEvent

    data class NavigateToSend(
        val cvAccount: CoinviewAccount
    ) : CoinviewNavigationEvent

    data class NavigateToReceive(
        val cvAccount: CoinviewAccount
    ) : CoinviewNavigationEvent

    data class NavigateToSwap(
        val cvAccount: CoinviewAccount
    ) : CoinviewNavigationEvent

    data class ShowRecurringBuyInfo(
        val recurringBuyId: String
    ) : CoinviewNavigationEvent
}
