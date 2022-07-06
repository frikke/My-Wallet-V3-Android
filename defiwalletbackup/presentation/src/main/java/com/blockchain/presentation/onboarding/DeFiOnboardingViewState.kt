package com.blockchain.presentation.onboarding

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class DeFiOnboardingViewState(
    val flowState: FlowState,
    val shouldLaunchPinVerification: Boolean
) : ViewState

sealed interface FlowState {
    object InProgress : FlowState
    data class Ended(val isSuccessful: Boolean) : FlowState
}
