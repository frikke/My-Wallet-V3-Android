package com.blockchain.chrome

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface MultiAppNavigationEvent : NavigationEvent {
    data class PhraseRecovery(val walletActivationRequired: Boolean) : MultiAppNavigationEvent
    object AppRating : MultiAppNavigationEvent
}
