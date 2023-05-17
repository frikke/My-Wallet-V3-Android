package com.blockchain.chrome

import androidx.lifecycle.viewModelScope
import com.blockchain.chrome.composable.bottomNavigationItems
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.featureflag.FeatureFlag
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
import piuk.blockchain.android.rating.domain.service.AppRatingService

class MultiAppViewModel(
    private val walletModeService: WalletModeService,
    private val walletModeBalanceService: WalletModeBalanceService,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val walletModePrefs: WalletModePrefs,
    private val dexFeatureFlag: FeatureFlag,
    private val appRatingService: AppRatingService
) : MviViewModel<
    MultiAppIntents,
    MultiAppViewState,
    MultiAppModelState,
    MultiAppNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    MultiAppModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun MultiAppModelState.reduce() = MultiAppViewState(
        modeSwitcherOptions = walletModes?.let {
            if (walletModes.size == 1) {
                ChromeModeOptions.SingleSelection(walletModes.first())
            } else {
                ChromeModeOptions.MultiSelection(walletModes)
            }
        },
        selectedMode = selectedWalletMode,
        backgroundColors = selectedWalletMode?.backgroundColors(),
        totalBalance = totalBalance.map { balance -> balance.toStringWithSymbol() },
        shouldRevealBalance = balanceRevealed.not(),
        bottomNavigationItems = selectedWalletMode?.bottomNavigationItems()?.filter {
            it != ChromeBottomNavigationItem.Dex || dexEnabled
        },
        selectedBottomNavigationItem = selectedBottomNavigationItem
    )

    override suspend fun handleIntent(modelState: MultiAppModelState, intent: MultiAppIntents) {
        when (intent) {
            MultiAppIntents.LoadData -> {
                viewModelScope.launch {
                    walletModeService.availableModes().let { availableModes ->
                        updateState {
                            copy(walletModes = availableModes)
                        }

                        if (availableModes.size == 1) {
                            walletModeService.updateEnabledWalletMode(availableModes.first())

                            updateState {
                                copy(selectedWalletMode = availableModes.first())
                            }
                        } else {
                            // collect wallet mode changes rather than manually change it when user switches modes
                            // as there are cases where it will change automatically
                            walletModeService.walletMode.collectLatest { walletMode ->
                                updateState {
                                    copy(selectedWalletMode = walletMode)
                                }
                            }
                        }
                    }
                }
                loadDex()
                loadBalance()
            }

            is MultiAppIntents.WalletModeSelected -> {
                walletModeService.updateEnabledWalletMode(intent.walletMode)

                if (intent.walletMode == WalletMode.NON_CUSTODIAL && shouldOnboardWalletForMode(intent.walletMode)) {
                    navigate(MultiAppNavigationEvent.DefiOnboarding)
                }
            }

            is MultiAppIntents.BalanceRevealed -> {
                updateState {
                    copy(balanceRevealed = true)
                }
            }

            is MultiAppIntents.BottomNavigationItemSelected -> {
                updateState {
                    copy(selectedBottomNavigationItem = intent.item)
                }
            }
        }
    }

    private fun loadDex() {
        viewModelScope.launch {
            val dexEnabled = dexFeatureFlag.coEnabled()
            updateState {
                copy(dexEnabled = dexEnabled)
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
                        copy(totalBalance = totalBalanceDataResource)
                    }

                    if (modelState.checkAppRating &&
                        appRatingService.shouldShowRating() &&
                        totalBalanceDataResource.map { it.isPositive }.dataOrElse(false)
                    ) {
                        navigate(MultiAppNavigationEvent.AppRating)

                        updateState {
                            copy(checkAppRating = false)
                        }
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
                .let {
                    updateState {
                        copy(defiBalance = defiBalance.updateDataWith(it))
                    }
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
            !walletStatusPrefs.hasSeenDefiOnboarding &&
            !walletModePrefs.userDefaultedToPKW
    }
}
