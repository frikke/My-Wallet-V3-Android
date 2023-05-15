package com.blockchain.home.presentation.allassets

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class EmptyScreenViewModel(
    private val homeAssetsViewModel: AssetsViewModel,
    private val pkwActivityViewModel: PrivateKeyActivityViewModel,
    private val walletModeService: WalletModeService,
    private val custodialActivityViewModel: CustodialActivityViewModel
) : MviViewModel<
    EmptyScreenIntent,
    EmptyScreenViewState,
    EmptyScreenModelState,
    EmptyScreenNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = EmptyScreenModelState(
        hasActivity = DataResource.Loading,
        hasAssets = DataResource.Loading,
        walletMode = WalletMode.CUSTODIAL
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: EmptyScreenModelState): EmptyScreenViewState {
        return with(state) {
            val hasOrMayHasAssets = hasAssets.dataOrElse(true)
            val hasOrMayHasActivity = hasActivity.dataOrElse(true)
            EmptyScreenViewState(
                show = !hasOrMayHasAssets && !hasOrMayHasActivity,
                mode = walletMode
            )
        }
    }

    override suspend fun handleIntent(modelState: EmptyScreenModelState, intent: EmptyScreenIntent) {
        when (intent) {
            EmptyScreenIntent.CheckEmptyState -> {
                viewModelScope.launch {
                    walletModeService.walletMode.flatMapLatest {
                        updateState { modelState ->
                            modelState.copy(
                                walletMode = it
                            )
                        }
                        when (it) {
                            WalletMode.NON_CUSTODIAL -> pkwActivityViewModel.viewState
                            WalletMode.CUSTODIAL -> custodialActivityViewModel.viewState
                        }
                    }.collect { viewState ->
                        updateState { state ->
                            state.copy(
                                hasActivity = viewState.activity.map { activities ->
                                    activities.any { act -> act.value.isNotEmpty() }
                                }
                            )
                        }
                    }
                }
                viewModelScope.launch {
                    homeAssetsViewModel.viewState.collect { state ->
                        updateState {
                            it.copy(
                                hasAssets = state.assets.map { assets ->
                                    assets.isNotEmpty()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class EmptyScreenViewState(val show: Boolean, val mode: WalletMode) : ViewState
data class EmptyScreenModelState(
    val hasActivity: DataResource<Boolean>,
    val walletMode: WalletMode,
    val hasAssets: DataResource<Boolean>
) : ModelState

class EmptyScreenNavigationEvent : NavigationEvent
sealed class EmptyScreenIntent : Intent<EmptyScreenModelState> {
    object CheckEmptyState : EmptyScreenIntent()
}
