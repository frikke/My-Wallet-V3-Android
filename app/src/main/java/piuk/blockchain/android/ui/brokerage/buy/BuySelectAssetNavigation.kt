package piuk.blockchain.android.ui.brokerage.buy

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import info.blockchain.balance.AssetInfo

sealed interface BuySelectAssetNavigation : NavigationEvent {
    data class PendingOrders(val maxTransactions: Int) : BuySelectAssetNavigation
    data class SimpleBuy(val assetInfo: AssetInfo) : BuySelectAssetNavigation
}
