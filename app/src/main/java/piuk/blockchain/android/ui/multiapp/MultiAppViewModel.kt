package piuk.blockchain.android.ui.multiapp

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MultiAppViewModel(
    private val walletModeService: WalletModeService
) : MviViewModel<
    MultiAppIntents,
    MultiAppViewState,
    MultiAppModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs>(
    MultiAppModelState(
        selectedWalletMode = walletModeService.enabledWalletMode().let {
            // necessary for KoinGraphTest to pass because Universal is default and reduce raises an error for it
            if (it == WalletMode.UNIVERSAL) WalletMode.CUSTODIAL_ONLY else it
        }
    )
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
            // collect wallet mode changes rather than manually change it when user switches modes
            // as there are cases where it will change automatically
            walletModeService.walletMode.collectLatest { walletMode ->
                updateState {
                    it.copy(selectedWalletMode = walletMode)
                }
            }
        }
    }

    override fun reduce(state: MultiAppModelState): MultiAppViewState = state.run {
        MultiAppViewState(
            modeSwitcherOptions = state.walletModes,
            selectedMode = state.selectedWalletMode,
            backgroundColors = when (state.selectedWalletMode) {
                WalletMode.CUSTODIAL_ONLY -> ChromeBackgroundColors.Trading
                WalletMode.NON_CUSTODIAL_ONLY -> ChromeBackgroundColors.DeFi
                WalletMode.UNIVERSAL -> error("WalletMode.UNIVERSAL unsupported")
            }
        )
    }

    override suspend fun handleIntent(modelState: MultiAppModelState, intent: MultiAppIntents) {
        when (intent) {
            is MultiAppIntents.WalletModeChanged -> {
                // update wallet mode - since we observe it in viewCreated level
                // it will automatically be updated
                walletModeService.updateEnabledWalletMode(intent.walletMode)
            }
        }
    }
}
