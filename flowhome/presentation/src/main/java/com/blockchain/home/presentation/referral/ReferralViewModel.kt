package com.blockchain.home.presentation.referral

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.updateDataWith
import com.blockchain.domain.referral.ReferralService
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ReferralViewModel(
    private val referralService: ReferralService
) : MviViewModel<
    ReferralIntent,
    ReferralViewState,
    ReferralModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(
    ReferralModelState()
) {
    private var referralJob: Job? = null
    private var resetCopyConfirmationDelayJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: ReferralModelState): ReferralViewState = state.run {
        ReferralViewState(
            referralInfo = referralInfo,
            showCodeCopyConfirmation = codeCopied
        )
    }

    override suspend fun handleIntent(modelState: ReferralModelState, intent: ReferralIntent) {
        when (intent) {
            ReferralIntent.LoadData -> loadData()
            ReferralIntent.CodeCopied -> handleCodeCopied()
        }
    }

    private fun loadData() {
        referralJob?.cancel()
        referralJob = viewModelScope.launch {
            referralService.fetchReferralData()
                .onEach { dataResource ->
                    updateState {
                        it.copy(referralInfo = it.referralInfo.updateDataWith(dataResource))
                    }
                }
                .collect()
        }
    }

    private fun handleCodeCopied() {
        updateState { it.copy(codeCopied = true) }

        resetCopyConfirmationDelayJob?.cancel()
        resetCopyConfirmationDelayJob = viewModelScope.launch {
            delay(COPY_CONFIRMATION_TIMEOUT)
            updateState { it.copy(codeCopied = false) }
        }
    }

    companion object {
        private const val COPY_CONFIRMATION_TIMEOUT = 3 * 1000L
    }
}
