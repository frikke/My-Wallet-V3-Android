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
import com.blockchain.walletconnect.ui.composable.DappSessionUiElement
import info.blockchain.balance.CryptoCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeDappsViewModel(
    private val sessionsRepository: SessionRepository,
    private val walletConnectService: WalletConnectServiceAPI,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2FeatureFlag: FeatureFlag
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
    override fun reduce(state: HomeDappsModelState): HomeDappsViewState {
        return when (state.connectedSessions) {
            is DataResource.Loading -> HomeDappsViewState.Loading
            is DataResource.Data -> {
                val sessions = state.connectedSessions.data
                if (sessions.isEmpty()) {
                    HomeDappsViewState.NoSessions
                } else {
                    val dappSessions = sessions.map {
                        reduceWalletConnectSession(it)
                    }
                    HomeDappsViewState.HomeDappsSessions(dappSessions)
                }
            }

            is DataResource.Error -> {
                HomeDappsViewState.NoSessions
            }
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

    private fun reduceWalletConnectSession(session: WalletConnectSession): DappSessionUiElement {
        return DappSessionUiElement(
            dappName = session.dAppInfo.peerMeta.name,
            dappUrl = session.dAppInfo.peerMeta.url.substringAfter("https://"),
            dappLogoUrl = session.dAppInfo.peerMeta.icons.firstOrNull().orEmpty(),
            chainName = CryptoCurrency.ETHER.name, // TODO support other ERC20 chains
            chainLogo = CryptoCurrency.ETHER.logo // TODO support other ERC20 chains
        )
    }

    private fun loadSessions() = viewModelScope.launch {
        if (walletConnectV2FeatureFlag.coEnabled()) {
            val sessionsV1Flow = sessionsRepository.retrieve()
                .onErrorReturn { emptyList() }.asFlow()

            val sessionsV2Flow = walletConnectV2Service.getSessionsFlow()

            combine(sessionsV1Flow, sessionsV2Flow) { sessionsV1, sessionsV2 ->
                sessionsV1 + sessionsV2
            }.collectLatest { sessions ->
                Timber.d("Loaded WalletConnect sessions: $sessions")
                updateState {
                    it.copy(connectedSessions = DataResource.Data(sessions))
                }
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
