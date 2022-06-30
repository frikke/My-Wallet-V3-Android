package piuk.blockchain.android.ui.dashboard.walletmode

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface WalletModeSelectionNavigationEvent : NavigationEvent {
    object DeFiOnboarding : WalletModeSelectionNavigationEvent
}
