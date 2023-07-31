package com.blockchain.home.presentation.dapps

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeDappsViewModel(
    private val sessionsRepository: SessionRepository,
    private val walletConnectService: WalletConnectServiceAPI,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2FeatureFlag: FeatureFlag,
    private val walletConnectV1FeatureFlag: FeatureFlag
) : MviViewModel<
    HomeDappsIntent,
    HomeDappsViewState,
    HomeDappsModelState,
    HomeDappsNavEvent,
    ModelConfigArgs.NoArgs>(
    HomeDappsModelState()
) {

    private var sessionsJob = loadSessions()

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun HomeDappsModelState.reduce() = when (connectedSessions) {
        is DataResource.Loading -> HomeDappsViewState.Loading
        is DataResource.Data -> {
            val sessions = connectedSessions.data
            if (sessions.isEmpty()) {
                HomeDappsViewState.NoSessions
            } else {
                val dappSessions = sessions.map {
                    it.toDappSessionUiElement()
                }
                HomeDappsViewState.HomeDappsSessions(dappSessions)
            }
        }

        is DataResource.Error -> {
            HomeDappsViewState.NoSessions
        }
    }

    override suspend fun handleIntent(modelState: HomeDappsModelState, intent: HomeDappsIntent) {
        when (intent) {
            is HomeDappsIntent.LoadData -> {
                sessionsJob.cancel()
                sessionsJob = loadSessions()
            }
        }
    }

    private fun loadSessions() = viewModelScope.launch {
        val sessionsV1Flow =
            if (walletConnectV1FeatureFlag.coEnabled())
                sessionsRepository.retrieve().onErrorReturn { emptyList() }.asFlow()
            else flowOf(emptyList())

        val sessionsV2Flow =
            if (walletConnectV2FeatureFlag.coEnabled())
                walletConnectV2Service.getSessionsFlow()
            else emptyFlow()

        combine(sessionsV1Flow, sessionsV2Flow) { sessionsV1, sessionsV2 ->
            sessionsV1 + sessionsV2
        }.collectLatest { sessions ->
            Timber.d("Loaded WalletConnect sessions: $sessions")
            updateState {
                copy(connectedSessions = DataResource.Data(sessions.take(5)))
            }
        }
    }
}

data class HomeDappsModelState(
    val connectedSessions: DataResource<List<WalletConnectSession>> = DataResource.Loading
) : ModelState

sealed class HomeDappsViewState : ViewState {
    object Loading : HomeDappsViewState()
    object NoSessions : HomeDappsViewState()
    data class HomeDappsSessions(val connectedSessions: List<DappSessionUiElement> = emptyList()) : HomeDappsViewState()
}

sealed interface HomeDappsNavEvent : NavigationEvent

sealed interface HomeDappsIntent : Intent<HomeDappsModelState> {
    object LoadData : HomeDappsIntent
}
