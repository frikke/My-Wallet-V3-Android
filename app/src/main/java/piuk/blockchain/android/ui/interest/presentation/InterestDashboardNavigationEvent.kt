package piuk.blockchain.android.ui.interest.presentation

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface InterestDashboardNavigationEvent : NavigationEvent {
    data class InterestSummary(val account: CryptoAccount) : InterestDashboardNavigationEvent
    data class InterestDeposit(val account: CryptoAccount) : InterestDashboardNavigationEvent
    object StartKyc : InterestDashboardNavigationEvent
}
