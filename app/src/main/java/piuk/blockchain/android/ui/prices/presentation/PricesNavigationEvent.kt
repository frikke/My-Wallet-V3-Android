package piuk.blockchain.android.ui.prices.presentation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import info.blockchain.balance.AssetInfo

sealed interface PricesNavigationEvent : NavigationEvent {
    data class CoinView(val assetInfo: AssetInfo) : PricesNavigationEvent
}
