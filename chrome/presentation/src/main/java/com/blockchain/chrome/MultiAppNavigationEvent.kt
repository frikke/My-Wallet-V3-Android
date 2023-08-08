package com.blockchain.chrome

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.walletmode.WalletMode

sealed interface MultiAppNavigationEvent : NavigationEvent {
    data class WalletIntro(val walletMode: WalletMode) : MultiAppNavigationEvent
    object AppRating : MultiAppNavigationEvent
}
