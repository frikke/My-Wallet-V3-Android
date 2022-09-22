package piuk.blockchain.android.ui.multiapp

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.combineDataResources
import com.blockchain.data.map
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.total
import java.util.stream.Collectors.toList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache

class MultiAppViewModel(
    private val walletModeService: WalletModeService,
    private val balanceStore: WalletModeBalanceCache
) : MviViewModel<
    MultiAppIntents,
    MultiAppViewState,
    MultiAppModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs>(
    MultiAppModelState(
        walletModes = walletModeService.availableModes(),
        selectedWalletMode = walletModeService.enabledWalletMode()
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

        loadTotalBalance()
    }

    override fun reduce(state: MultiAppModelState): MultiAppViewState = state.run {
        MultiAppViewState(
            modeSwitcherOptions = state.walletModes,
            selectedMode = state.selectedWalletMode,
            backgroundColors = when (state.selectedWalletMode) {
                WalletMode.CUSTODIAL_ONLY -> ChromeBackgroundColors.Trading
                WalletMode.NON_CUSTODIAL_ONLY -> ChromeBackgroundColors.DeFi
                WalletMode.UNIVERSAL -> error("WalletMode.UNIVERSAL unsupported")
            },
            totalBalance = state.totalBalance.map { balance -> balance.toStringWithSymbol() }
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

    private fun loadTotalBalance() {
        viewModelScope.launch {
            val balances = modelState.walletModes.map { walletMode ->
                balanceStore
                    .stream(FreshnessStrategy.Cached(forceRefresh = true).withKey(walletMode))
                    .mapData { it.total }
            }

            combine(balances) { balancesArray ->
                combineDataResources(balancesArray.toList()) { balancesList ->
                    balancesList.total()
                }
            }.collectLatest { totalBalanceDataResource ->
                updateState {
                    it.copy(totalBalance = totalBalanceDataResource)
                }
            }
        }
    }
}
