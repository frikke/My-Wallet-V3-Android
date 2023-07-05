package com.blockchain.home.presentation.failedbalances

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FailedBalancesViewModel(
    private val unifiedBalancesService: UnifiedBalancesService
) : MviViewModel<FailedBalancesIntent,
    FailedBalancesViewState,
    FailedBalancesModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs>(
    FailedBalancesModelState()
) {
    private var failedNetworksJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun FailedBalancesModelState.reduce() = FailedBalancesViewState(
        failedNetworkNames = failedBalancesCurrencies.mapList { it.name },
        dismissWarning = dismissFailedNetworksWarning
    )

    override suspend fun handleIntent(modelState: FailedBalancesModelState, intent: FailedBalancesIntent) {
        when (intent) {
            is FailedBalancesIntent.LoadData -> {
                loadFailedNetworks(forceRefresh = false)
            }

            FailedBalancesIntent.DismissFailedNetworksWarning -> {
                updateState {
                    copy(dismissFailedNetworksWarning = true)
                }
            }

            FailedBalancesIntent.Refresh -> {
                loadFailedNetworks(forceRefresh = true)
            }
        }
    }

    private fun loadFailedNetworks(forceRefresh: Boolean) {
        failedNetworksJob?.cancel()
        failedNetworksJob = viewModelScope.launch {
            unifiedBalancesService.failedBalancesCurrencies(
                freshnessStrategy = PullToRefresh.freshnessStrategy(shouldGetFresh = forceRefresh)
            ).collectLatest {
                updateState {
                    copy(failedBalancesCurrencies = failedBalancesCurrencies.updateDataWith(it))
                }
            }
        }
    }
}