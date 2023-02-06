package piuk.blockchain.android.ui.dashboard.walletmode

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.extensions.exhaustive
import com.blockchain.preferences.WalletModePrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeBalanceService
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import piuk.blockchain.android.R

class WalletModeSelectionViewModel(
    private val walletModeService: WalletModeService,
    private val walletModeBalanceService: WalletModeBalanceService,
    private val payloadManager: PayloadDataManager,
    private val walletModePrefs: WalletModePrefs,
    walletStatusPrefs: WalletStatusPrefs,
) :
    MviViewModel<
        WalletModeSelectionIntent,
        WalletModeSelectionViewState,
        WalletModeSelectionModelState,
        WalletModeSelectionNavigationEvent,
        ModelConfigArgs.NoArgs>(
        initialState = WalletModeSelectionModelState(
            isWalletBackedUp = false,
            isWalletBackUpSkipped = walletStatusPrefs.isWalletBackUpSkipped,
            brokerageBalance = null,
            anyBrokerageBalanceFailed = false,
            defiBalance = null,
            anyDefiBalanceFailed = false,
        )
    ) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: WalletModeSelectionModelState): WalletModeSelectionViewState {
        with(state) {
            return WalletModeSelectionViewState(
                totalBalance = totalBalance(brokerageBalance, defiBalance),
                brokerageBalance = brokerageBalance?.let {
                    BalanceState.Data(it)
                } ?: BalanceState.Loading,
                showBrokerageBalanceWarning = anyBrokerageBalanceFailed,
                defiWalletBalance = defiBalance?.let {
                    if (state.enabledWalletMode == WalletMode.NON_CUSTODIAL) {
                        BalanceState.Data(it)
                    } else if (walletModePrefs.userDefaultedToPKW && shouldBackupPhraseForMode(
                            WalletMode.NON_CUSTODIAL
                        )
                    ) {
                        BalanceState.PhraseRecoveryRequired(
                            activationRequired = false,
                            balance = it
                        )
                    } else if (shouldOnboardWalletForMode(WalletMode.NON_CUSTODIAL)) {
                        BalanceState.PhraseRecoveryRequired(
                            activationRequired = true,
                            balance = it
                        )
                    } else {
                        BalanceState.Data(it)
                    }
                } ?: BalanceState.Loading,
                showDefiBalanceWarning = anyDefiBalanceFailed,
                defiWalletAvailable = true,
                enabledWalletMode = enabledWalletMode,
            )
        }
    }

    private fun totalBalance(portfolioBalance: Money?, defiBalance: Money?): BalanceState {
        val portfBalance = portfolioBalance ?: return BalanceState.Loading
        val defBalance = defiBalance ?: return BalanceState.Loading
        if (portfBalance.currency.networkTicker == defBalance.currency.networkTicker)
            return BalanceState.Data(portfBalance.plus(defBalance))
        return BalanceState.Loading
    }

    override suspend fun handleIntent(modelState: WalletModeSelectionModelState, intent: WalletModeSelectionIntent) {
        when (intent) {
            WalletModeSelectionIntent.LoadInitialData -> {
                // isBackedUp
                updateState {
                    it.copy(isWalletBackedUp = payloadManager.isBackedUp)
                }

                // balances
                updateState {
                    it.copy(brokerageBalance = null, defiBalance = null)
                }

                val nonCustodialBalance = walletModeBalanceService.getBalanceWithFailureState(
                    WalletMode.NON_CUSTODIAL,
                    FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
                ).map { response ->
                    when (response) {
                        is DataResource.Data -> updateState {
                            it.copy(
                                defiBalance = response.data.first,
                                anyDefiBalanceFailed = response.data.second
                            )
                        }
                        is DataResource.Error,
                        DataResource.Loading,
                        -> {
                            // Do nothing
                        }
                    }
                }

                val custodialBalance = walletModeBalanceService.getBalanceWithFailureState(
                    WalletMode.CUSTODIAL,
                    FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
                ).map { response ->
                    when (response) {
                        is DataResource.Data -> updateState {
                            it.copy(
                                brokerageBalance = response.data.first,
                                anyBrokerageBalanceFailed = response.data.second
                            )
                        }
                        is DataResource.Error,
                        DataResource.Loading,
                        -> {
                            // Do nothing
                        }
                    }
                }
                val walletMode = walletModeService.walletMode.onEach {
                    updateState { state ->
                        state.copy(enabledWalletMode = it)
                    }
                }
                merge(nonCustodialBalance, custodialBalance, walletMode).collect()
            }

            is WalletModeSelectionIntent.ActivateWalletModeRequested -> {
                if (modelState.shouldBackupPhraseForMode(intent.walletMode)) { // /
                    navigate(
                        WalletModeSelectionNavigationEvent.PhraseRecovery(
                            onboardingRequired = modelState.shouldOnboardWalletForMode(intent.walletMode)
                        )
                    )
                } else {
                    updateActiveWalletMode(intent.walletMode)
                }
            }

            WalletModeSelectionIntent.DeFiOnboardingComplete -> {
                updateActiveWalletMode(WalletMode.NON_CUSTODIAL)
            }
        }.exhaustive
    }

    private fun updateActiveWalletMode(walletMode: WalletMode) {
        updateState {
            it.copy(enabledWalletMode = walletMode)
        }
        viewModelScope.launch {
            walletModeService.updateEnabledWalletMode(walletMode)
        }
        navigate(WalletModeSelectionNavigationEvent.Close(walletMode = walletMode))
    }

    private fun WalletModeSelectionModelState.shouldBackupPhraseForMode(walletMode: WalletMode): Boolean {
        return when (walletMode) {
            WalletMode.NON_CUSTODIAL -> isWalletBackedUp.not() && isWalletBackUpSkipped.not()
            else -> false
        }
    }

    private fun WalletModeSelectionModelState.shouldOnboardWalletForMode(walletMode: WalletMode): Boolean {
        val isWalletEligibleForActivation = when (walletMode) {
            WalletMode.NON_CUSTODIAL -> defiBalance?.isZero == true
            else -> false
        }
        return isWalletEligibleForActivation &&
            shouldBackupPhraseForMode(walletMode) &&
            !walletModePrefs.userDefaultedToPKW
    }
}

sealed class WalletModeSelectionIntent : Intent<WalletModeSelectionModelState> {
    object LoadInitialData : WalletModeSelectionIntent()

    data class ActivateWalletModeRequested(val walletMode: WalletMode) : WalletModeSelectionIntent() {
        override fun isValidFor(modelState: WalletModeSelectionModelState): Boolean {
            return modelState.enabledWalletMode != walletMode
        }
    }

    object DeFiOnboardingComplete : WalletModeSelectionIntent()
}

data class WalletModeSelectionViewState(
    val totalBalance: BalanceState,
    val brokerageBalance: BalanceState,
    val showBrokerageBalanceWarning: Boolean,
    val defiWalletBalance: BalanceState,
    val showDefiBalanceWarning: Boolean,
    val defiWalletAvailable: Boolean,
    val enabledWalletMode: WalletMode?,
) : ViewState

data class WalletModeSelectionModelState(
    val isWalletBackedUp: Boolean,
    val isWalletBackUpSkipped: Boolean,
    val brokerageBalance: Money?,
    val anyBrokerageBalanceFailed: Boolean,
    val defiBalance: Money?,
    val anyDefiBalanceFailed: Boolean,
    val enabledWalletMode: WalletMode? = null,
) : ModelState

sealed class BalanceState {
    object Loading : BalanceState()
    data class Data(val money: Money) : BalanceState()
    data class PhraseRecoveryRequired(val balance: Money, val activationRequired: Boolean) : BalanceState()
}

sealed interface WalletModeSelectionNavigationEvent : NavigationEvent {
    data class PhraseRecovery(val onboardingRequired: Boolean) : WalletModeSelectionNavigationEvent
    data class Close(val walletMode: WalletMode) : WalletModeSelectionNavigationEvent
}

@StringRes
fun WalletMode.title(): Int = when (this) {
    WalletMode.NON_CUSTODIAL -> R.string.defi_wallet_name
    WalletMode.CUSTODIAL -> R.string.brokerage_wallet_name
    else -> throw IllegalArgumentException("No title supported for mode")
}

@DrawableRes
fun WalletMode.icon(): Int = when (this) {
    WalletMode.NON_CUSTODIAL -> R.drawable.ic_defi_wallet
    WalletMode.CUSTODIAL -> R.drawable.ic_portfolio
    else -> throw IllegalArgumentException("No icon supported for mode")
}
