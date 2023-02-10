package com.blockchain.chrome

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface MultiAppNavigationEvent : NavigationEvent {
    object DefiOnboarding : MultiAppNavigationEvent
    object AppRating : MultiAppNavigationEvent
}
