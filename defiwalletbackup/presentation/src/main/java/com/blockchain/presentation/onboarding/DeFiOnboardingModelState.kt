package com.blockchain.presentation.onboarding

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class DeFiOnboardingModelState(
    val flowState: FlowState = FlowState.InProgress,
    val phraseBackupRequired: Boolean = false
) : ModelState
