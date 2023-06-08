package com.blockchain.walletconnect.ui.dapps.v2

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.walletconnect.domain.WalletConnectAuthSigningPayload
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import kotlinx.coroutines.flow.collectLatest

class WalletConnectAuthRequestViewModel(
    private val walletConnectV2Service: WalletConnectV2Service
) : MviViewModel<
    WalletConnectAuthRequestIntent,
    WalletConnectAuthRequestViewState,
    WalletConnectAuthRequestModelState,
    WalletConnectAuthRequestNavEvent,
    ModelConfigArgs.NoArgs>(
    WalletConnectAuthRequestModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    override fun WalletConnectAuthRequestModelState.reduce() =
        when (authSigningPayload) {
            is DataResource.Error,
            is DataResource.Loading -> WalletConnectAuthRequestViewState.WalletConnectAuthRequestLoading
            is DataResource.Data -> {
                WalletConnectAuthRequestViewState.WalletConnectAuthRequestData(
                    domain = authSigningPayload.data.domain,
                    authMessage = authSigningPayload.data.authMessage
                )
            }
        }

    override suspend fun handleIntent(
        modelState: WalletConnectAuthRequestModelState,
        intent: WalletConnectAuthRequestIntent
    ) {
        when (intent) {
            is WalletConnectAuthRequestIntent.LoadAuthRequest -> {
                walletConnectV2Service.buildAuthSigningPayload(intent.authId).collectLatest { authSigningPayload ->
                    updateState {
                        copy(
                            authId = intent.authId,
                            authSigningPayload = DataResource.Data(authSigningPayload),
                        )
                    }
                }
            }

            WalletConnectAuthRequestIntent.ApproveAuth -> {
                if (modelState.authSigningPayload is DataResource.Data) {
                    walletConnectV2Service.approveAuthRequest(modelState.authSigningPayload.data)
                }
            }
            WalletConnectAuthRequestIntent.RejectAuth -> {
                walletConnectV2Service.rejectAuthRequest(modelState.authId)
            }
        }
    }
}

data class WalletConnectAuthRequestModelState(
    val authId: String = "",
    val authSigningPayload: DataResource<WalletConnectAuthSigningPayload> = DataResource.Loading,
) : ModelState

sealed class WalletConnectAuthRequestViewState : ViewState {
    data class WalletConnectAuthRequestData(
        val domain: String,
        val authMessage: String
    ) : WalletConnectAuthRequestViewState()

    object WalletConnectAuthRequestLoading : WalletConnectAuthRequestViewState()
}

sealed interface WalletConnectAuthRequestNavEvent : NavigationEvent

sealed interface WalletConnectAuthRequestIntent : Intent<WalletConnectAuthRequestModelState> {
    data class LoadAuthRequest(val authId: String) : WalletConnectAuthRequestIntent
    object ApproveAuth : WalletConnectAuthRequestIntent
    object RejectAuth : WalletConnectAuthRequestIntent
}
