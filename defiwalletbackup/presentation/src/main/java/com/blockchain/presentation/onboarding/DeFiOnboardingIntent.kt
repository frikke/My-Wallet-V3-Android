package com.blockchain.presentation.onboarding

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface DeFiOnboardingIntent : Intent<DeFiOnboardingModelState> {
    // intro
    object EnableDeFiWallet : DeFiOnboardingIntent
    object PhraseBackupRequested : DeFiOnboardingIntent

    // flow
    data class EndFlow(val isSuccessful: Boolean) : DeFiOnboardingIntent
}
