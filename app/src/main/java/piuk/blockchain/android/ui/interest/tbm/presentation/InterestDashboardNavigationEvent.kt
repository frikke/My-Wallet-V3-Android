package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface InterestDashboardNavigationEvent : NavigationEvent {
    data class NavigateToInterestSummarySheet(val account: CryptoAccount) : InterestDashboardNavigationEvent
    data class NavigateToTransactionFlow(val account: CryptoAccount) : InterestDashboardNavigationEvent
}