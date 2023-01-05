package com.blockchain.presentation.onboarding.navigation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface DeFiOnboardingNavigationEvent : NavigationEvent {
    object DeFiOnboardingIntro : DeFiOnboardingNavigationEvent
}
