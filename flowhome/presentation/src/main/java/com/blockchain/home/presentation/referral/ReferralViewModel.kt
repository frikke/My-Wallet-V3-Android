package com.blockchain.home.presentation.referral

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.updateDataWith
import com.blockchain.domain.referral.ReferralService
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.utils.CurrentTimeProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
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
    ModelConfigArgs.NoArgs
    >(
    ReferralModelState()
) {
    private var referralJob: Job? = null
    private var resetCopyConfirmationDelayJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun ReferralModelState.reduce() = ReferralViewState(
        referralInfo = referralInfo,
        showCodeCopyConfirmation = codeCopied
    )

    override suspend fun handleIntent(modelState: ReferralModelState, intent: ReferralIntent) {
        when (intent) {
            is ReferralIntent.LoadData -> {
                loadData(intent.forceRefresh)
            }

            ReferralIntent.CodeCopied -> {
                handleCodeCopied()
            }

            ReferralIntent.Refresh -> {
                updateState {
                    copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }

                onIntent(ReferralIntent.LoadData(forceRefresh = true))
            }
        }
    }

    private fun loadData(forceRefresh: Boolean) {
        referralJob?.cancel()
        referralJob = viewModelScope.launch(Dispatchers.IO) {
            referralService.fetchReferralData(
                freshnessStrategy = PullToRefresh.freshnessStrategy(
                    forceRefresh,
                    RefreshStrategy.RefreshIfOlderThan(1, TimeUnit.HOURS)
                )
            )
                .onEach { dataResource ->
                    updateState {
                        copy(referralInfo = referralInfo.updateDataWith(dataResource))
                    }
                }
                .collect()
        }
    }

    private fun handleCodeCopied() {
        updateState { copy(codeCopied = true) }

        resetCopyConfirmationDelayJob?.cancel()
        resetCopyConfirmationDelayJob = viewModelScope.launch {
            delay(COPY_CONFIRMATION_TIMEOUT)
            updateState { copy(codeCopied = false) }
        }
    }

    companion object {
        private const val COPY_CONFIRMATION_TIMEOUT = 3 * 1000L
    }
}
