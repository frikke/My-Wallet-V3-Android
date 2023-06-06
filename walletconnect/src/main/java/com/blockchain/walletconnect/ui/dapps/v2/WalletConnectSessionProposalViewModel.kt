package com.blockchain.walletconnect.ui.dapps.v2

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.walletconnect.domain.WalletConnectSessionProposalState
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletConnectSessionProposalViewModel(
    private val walletConnectV2Service: WalletConnectV2Service
) : MviViewModel<
    WalletConnectSessionProposalIntent,
    WalletConnectSessionProposalViewState,
    WalletConnectSessionProposalModelState,
    WalletConnectSessionProposalNavEvent,
    ModelConfigArgs.NoArgs>(
    WalletConnectSessionProposalModelState()
) {

    private var loadSessionProposalJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    override fun WalletConnectSessionProposalModelState.reduce() =
        when (sessionState) {
            is DataResource.Error,
            is DataResource.Loading -> WalletConnectSessionProposalViewState(
                sessionState = null,
                dappName = dappName,
                dappDescription = dappDescription,
                dappLogoUrl = dappLogoUrl
            )
            is DataResource.Data -> {
                WalletConnectSessionProposalViewState(
                    sessionState = sessionState.data,
                    dappName = dappName,
                    dappDescription = dappDescription,
                    dappLogoUrl = dappLogoUrl
                )
            }
        }

    override suspend fun handleIntent(
        modelState: WalletConnectSessionProposalModelState,
        intent: WalletConnectSessionProposalIntent
    ) {
        when (intent) {
            is WalletConnectSessionProposalIntent.LoadSessionProposal -> {
                walletConnectV2Service.getSessionProposal(intent.sessionId)?.let { sessionProposal ->
                    updateState {
                        copy(
                            sessionId = intent.sessionId,
                            dappName = sessionProposal.name,
                            dappDescription = sessionProposal.description,
                            dappLogoUrl = sessionProposal.icons.firstOrNull()?.toString().orEmpty()
                        )
                    }
                } ?: run {
                    Timber.e("WalletConnectV2: Session proposal not found")
                }

                loadSessionProposalJob = viewModelScope.launch {
                    walletConnectV2Service.getSessionProposalState().collectLatest { sessionProposalState ->
                        when (sessionProposalState) {
                            WalletConnectSessionProposalState.APPROVED,
                            WalletConnectSessionProposalState.REJECTED -> {
                                updateState {
                                    copy(sessionState = DataResource.Data(sessionProposalState))
                                }
                                loadSessionProposalJob?.cancel()
                            }

                            else -> {}
                        }
                    }
                }
            }

            is WalletConnectSessionProposalIntent.ApproveSession -> {
                walletConnectV2Service.approveLastSession()
            }

            is WalletConnectSessionProposalIntent.RejectSession -> {
                walletConnectV2Service.clearSessionProposals()
                updateState {
                    copy(sessionState = DataResource.Data(WalletConnectSessionProposalState.REJECTED))
                }
            }
        }
    }
}

data class WalletConnectSessionProposalModelState(
    val sessionState: DataResource<WalletConnectSessionProposalState?> = DataResource.Loading,
    val sessionId: String = "",
    val dappName: String = "",
    val dappDescription: String = "",
    val dappLogoUrl: String = ""
) : ModelState

data class WalletConnectSessionProposalViewState(
    val sessionState: WalletConnectSessionProposalState? = null,
    val dappName: String = "",
    val dappDescription: String = "",
    val dappLogoUrl: String = ""
) : ViewState

sealed interface WalletConnectSessionProposalNavEvent : NavigationEvent

sealed interface WalletConnectSessionProposalIntent : Intent<WalletConnectSessionProposalModelState> {
    data class LoadSessionProposal(val sessionId: String) : WalletConnectSessionProposalIntent
    object ApproveSession : WalletConnectSessionProposalIntent
    object RejectSession : WalletConnectSessionProposalIntent
}
