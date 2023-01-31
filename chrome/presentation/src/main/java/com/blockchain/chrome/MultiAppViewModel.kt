package com.blockchain.chrome

import androidx.lifecycle.viewModelScope
import com.blockchain.chrome.composable.bottomNavigationItems
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeBalanceService
import com.blockchain.walletmode.WalletModeService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MultiAppViewModel(
    private val walletModeService: WalletModeService,
    private val walletModeBalanceService: WalletModeBalanceService,
    private val payloadManager: PayloadDataManager,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val walletModePrefs: WalletModePrefs
) : MviViewModel<
    MultiAppIntents,
    MultiAppViewState,
    MultiAppModelState,
    MultiAppNavigationEvent,
    ModelConfigArgs.NoArgs>(
    MultiAppModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: MultiAppModelState): MultiAppViewState = state.run {
        MultiAppViewState(
            modeSwitcherOptions = state.walletModes?.let {
                if (state.walletModes.size == 1) {
                    ChromeModeOptions.SingleSelection(state.walletModes.first())
                } else {
                    ChromeModeOptions.MultiSelection(state.walletModes)
                }
            },
            selectedMode = state.selectedWalletMode,
            backgroundColors = state.selectedWalletMode?.backgroundColors(),
            totalBalance = state.totalBalance.map { balance -> balance.toStringWithSymbol() },
            shouldRevealBalance = state.balanceRevealed.not(),
            bottomNavigationItems = state.selectedWalletMode?.bottomNavigationItems()
        )
    }

    override suspend fun handleIntent(modelState: MultiAppModelState, intent: MultiAppIntents) {
        when (intent) {
            MultiAppIntents.LoadData -> {
                viewModelScope.launch {
                    walletModeService.availableModes().let { availableModes ->
                        updateState {
                            it.copy(walletModes = availableModes)
                        }
                    }
                }

                viewModelScope.launch {
                    // collect wallet mode changes rather than manually change it when user switches modes
                    // as there are cases where it will change automatically
                    walletModeService.walletMode.collectLatest { walletMode ->
                        updateState {
                            it.copy(selectedWalletMode = walletMode)
                        }
                    }
                }

                loadBalance()
            }

            is MultiAppIntents.WalletModeChangeRequested -> {
                if (shouldBackupPhraseForMode(intent.walletMode)) {
                    navigate(
                        MultiAppNavigationEvent.PhraseRecovery(
                            walletActivationRequired = shouldOnboardWalletForMode(intent.walletMode)
                        )
                    )
                } else {
                    walletModeService.updateEnabledWalletMode(intent.walletMode)
                }
            }

            is MultiAppIntents.BalanceRevealed -> {
                updateState {
                    it.copy(balanceRevealed = true)
                }
            }
        }
    }

    private fun loadBalance() {
        // total balance to be shown on top
        viewModelScope.launch {
            walletModeBalanceService.totalBalance(
                freshnessStrategy =
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
            )
                .distinctUntilChanged()
                .debounce(1000)
                .collectLatest { totalBalanceDataResource ->
                    updateState {
                        it.copy(totalBalance = totalBalanceDataResource)
                    }
                }
        }

        // defi balance to be used in checking defi onboarding state
        viewModelScope.launch {
            walletModeBalanceService.balanceFor(
                walletMode = WalletMode.CUSTODIAL,
                freshnessStrategy =
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
            )
                .filterNot { it is DataResource.Loading }
                .first()
                .run {
                    updateState {
                        it.copy(defiBalance = it.defiBalance.updateDataWith(this))
                    }
                }
        }
    }

    private fun shouldBackupPhraseForMode(walletMode: WalletMode): Boolean {
        return when (walletMode) {
            WalletMode.NON_CUSTODIAL -> {
                payloadManager.isBackedUp.not() && walletStatusPrefs.isWalletBackUpSkipped.not()
            }
            else -> {
                false
            }
        }
    }

    private fun shouldOnboardWalletForMode(walletMode: WalletMode): Boolean {
        val isWalletEligibleForActivation = when (walletMode) {
            WalletMode.NON_CUSTODIAL -> {
                (modelState.defiBalance as? DataResource.Data)?.data?.isZero == true
            }
            else -> {
                false
            }
        }
        return isWalletEligibleForActivation &&
            shouldBackupPhraseForMode(walletMode) &&
            !walletModePrefs.userDefaultedToPKW
    }
}
