package com.blockchain.walletconnect.ui.dapps.v2

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.utils.asFlow
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletconnect.ui.composable.common.toDappSessionUiElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WalletConnectSessionDetailViewModel(
    private val sessionsRepository: SessionRepository,
    private val walletConnectService: WalletConnectServiceAPI,
    private val walletConnectV2Service: WalletConnectV2Service
) : MviViewModel<
    WalletConnectSessionDetailIntent,
    WalletConnectSessionDetailViewState,
    WalletConnectSessionDetailModelState,
    WalletConnectSessionDetailNavEvent,
    ModelConfigArgs.NoArgs>(
    WalletConnectSessionDetailModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    override fun WalletConnectSessionDetailModelState.reduce() =
        when (session) {
            is DataResource.Error,
            is DataResource.Loading -> WalletConnectSessionDetailViewState.Loading
            is DataResource.Data -> {
                val session = session.data
                WalletConnectSessionDetailViewState.WalletConnectSessionLoaded(session.toDappSessionUiElement())
            }
        }

    override suspend fun handleIntent(
        modelState: WalletConnectSessionDetailModelState,
        intent: WalletConnectSessionDetailIntent
    ) {
        when (intent) {
            is WalletConnectSessionDetailIntent.LoadSession -> {
                loadSession(intent.sessionId, isV2 = intent.isV2).collectLatest { session ->
                    if (session != null) {
                        updateState {
                            copy(session = DataResource.Data(session))
                        }
                    }
                }
            }

            is WalletConnectSessionDetailIntent.DisconnectSession -> {
                disconnectSession(
                    (modelState.session as DataResource.Data).data
                )
            }
        }
    }

    private suspend fun loadSession(sessionId: String, isV2: Boolean): Flow<WalletConnectSession?> {
        return if (isV2) {
            flowOf(walletConnectV2Service.getSession(sessionId))
        } else {
            sessionsRepository.retrieve().asFlow().map { sessions ->
                sessions.first { it.walletInfo.clientId == sessionId }
            }
        }
    }

    private suspend fun disconnectSession(session: WalletConnectSession) {
        if (session.isV2) {
            walletConnectV2Service.disconnectSession(session.walletInfo.clientId)
        } else {
            walletConnectService.disconnect(session).subscribe()
        }
    }
}

data class WalletConnectSessionDetailModelState(
    val session: DataResource<WalletConnectSession> = DataResource.Loading
) : ModelState

sealed class WalletConnectSessionDetailViewState : ViewState {
    object Loading : WalletConnectSessionDetailViewState()
    data class WalletConnectSessionLoaded(val session: DappSessionUiElement) : WalletConnectSessionDetailViewState()
}

sealed interface WalletConnectSessionDetailNavEvent : NavigationEvent

sealed interface WalletConnectSessionDetailIntent : Intent<WalletConnectSessionDetailModelState> {
    data class LoadSession(val sessionId: String, val isV2: Boolean) : WalletConnectSessionDetailIntent
    object DisconnectSession : WalletConnectSessionDetailIntent
}
