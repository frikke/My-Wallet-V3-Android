package com.blockchain.presentation.onboarding.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.onboarding.DeFiOnboardingIntent
import com.blockchain.presentation.onboarding.DeFiOnboardingModelState
import com.blockchain.presentation.onboarding.DeFiOnboardingViewState
import com.blockchain.presentation.onboarding.FlowState
import com.blockchain.presentation.onboarding.navigation.DeFiOnboardingNavigationEvent

class DeFiOnboardingViewModel : MviViewModel<DeFiOnboardingIntent,
    DeFiOnboardingViewState,
    DeFiOnboardingModelState,
    DeFiOnboardingNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = DeFiOnboardingModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: DeFiOnboardingModelState): DeFiOnboardingViewState {
        return with(state) {
            DeFiOnboardingViewState(
                flowState = flowState,
                shouldLaunchPhraseBackup = phraseBackupRequired
            )
        }
    }

    override suspend fun handleIntent(modelState: DeFiOnboardingModelState, intent: DeFiOnboardingIntent) {
        when (intent) {
            DeFiOnboardingIntent.EnableDeFiWallet -> {
                updateState { it.copy(phraseBackupRequired = true) }
            }

            DeFiOnboardingIntent.PhraseBackupRequested -> {
                updateState { it.copy(phraseBackupRequired = false) }
            }

            is DeFiOnboardingIntent.EndFlow -> {
                updateState { it.copy(flowState = FlowState.Ended(intent.isSuccessful)) }
            }
        }.exhaustive
    }
}
