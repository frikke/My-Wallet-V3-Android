package com.blockchain.walletconnect.ui.dapps.v2

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.utils.asFlow
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletconnect.ui.composable.common.toDappSessionUiElement
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletConnectDappListViewModel(
    private val sessionsRepository: SessionRepository,
    private val walletConnectService: WalletConnectServiceAPI,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2FeatureFlag: FeatureFlag
) : MviViewModel<
    WalletConnectDappListIntent,
    WalletConnectDappListViewState,
    WalletConnectDappListModelState,
    WalletConnectDappListNavEvent,
    ModelConfigArgs.NoArgs>(
    WalletConnectDappListModelState()
) {

    private var sessionsJob = loadSessions()

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    override fun WalletConnectDappListModelState.reduce() = when (connectedSessions) {
        is DataResource.Error,
        is DataResource.Loading -> WalletConnectDappListViewState.Loading
        is DataResource.Data -> {
            val dappSessions = connectedSessions.data.map {
                it.toDappSessionUiElement()
            }
            WalletConnectDappListViewState.WalletConnectDappListSessions(dappSessions)
        }
    }

    override suspend fun handleIntent(
        modelState: WalletConnectDappListModelState,
        intent: WalletConnectDappListIntent
    ) {
        when (intent) {
            is WalletConnectDappListIntent.LoadData -> {
                sessionsJob.cancel()
                sessionsJob = loadSessions()
            }
            is WalletConnectDappListIntent.DisconnectAllSessions -> {
                sessionsJob.cancel()

                // Disconnect V2 sessions
                walletConnectV2Service.disconnectAllSessions()

                // Disconnect V1 sessions
                if (modelState.connectedSessions is DataResource.Data) {
                    modelState.connectedSessions.data
                        .filter { !it.isV2 }
                        .map {
                            Timber.d("Disconnecting session: $it")
                            walletConnectService.disconnect(it).subscribe()
                        }
                }
            }
        }
    }

    private fun loadSessions() = viewModelScope.launch {
        val sessionsV1Flow = sessionsRepository.retrieve()
            .onErrorReturn { emptyList() }.asFlow()

        val sessionsV2Flow =
            if (walletConnectV2FeatureFlag.coEnabled()) walletConnectV2Service.getSessionsFlow()
            else emptyFlow()

        combine(sessionsV1Flow, sessionsV2Flow) { sessionsV1, sessionsV2 ->
            sessionsV1 + sessionsV2
        }.collectLatest { sessions ->
            Timber.d("Loaded WalletConnect sessions: $sessions")
            updateState {
                copy(connectedSessions = DataResource.Data(sessions))
            }
        }
    }
}

data class WalletConnectDappListModelState(
    val connectedSessions: DataResource<List<WalletConnectSession>> = DataResource.Loading
) : ModelState

sealed class WalletConnectDappListViewState : ViewState {
    object Loading : WalletConnectDappListViewState()
    data class WalletConnectDappListSessions(
        val connectedSessions: List<DappSessionUiElement> = emptyList()
    ) : WalletConnectDappListViewState()
}

sealed interface WalletConnectDappListNavEvent : NavigationEvent

sealed interface WalletConnectDappListIntent : Intent<WalletConnectDappListModelState> {
    object LoadData : WalletConnectDappListIntent
    object DisconnectAllSessions : WalletConnectDappListIntent
}
