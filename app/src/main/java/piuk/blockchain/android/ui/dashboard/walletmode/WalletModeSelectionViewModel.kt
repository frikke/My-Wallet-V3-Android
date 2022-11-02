package piuk.blockchain.android.ui.dashboard.walletmode

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.extensions.exhaustive
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache

class WalletModeSelectionViewModel(
    private val walletModeService: WalletModeService,
    private val cache: WalletModeBalanceCache,
    private val payloadManager: PayloadDataManager,
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
            enabledWalletMode = walletModeService.enabledWalletMode()
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
                    if (shouldActivateWalletForMode(WalletMode.NON_CUSTODIAL_ONLY)) {
                        BalanceState.ActivationRequired
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

                val nonCustodialBalance = cache.getBalanceWithFailureState(
                    WalletMode.NON_CUSTODIAL_ONLY,
                    FreshnessStrategy.Cached(forceRefresh = true)
                ).map { response ->
                    when (response) {
                        is DataResource.Data -> updateState {
                            it.copy(
                                defiBalance = response.data.first.total,
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

                val custodialBalance = cache.getBalanceWithFailureState(
                    WalletMode.CUSTODIAL_ONLY,
                    FreshnessStrategy.Cached(forceRefresh = true)
                ).map { response ->
                    when (response) {
                        is DataResource.Data -> updateState {
                            it.copy(
                                brokerageBalance = response.data.first.total,
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

                merge(nonCustodialBalance, custodialBalance).collect()
            }

            is WalletModeSelectionIntent.ActivateWalletModeRequested -> {
                if (modelState.shouldBackupPhraseForMode(intent.walletMode)) {
                    navigate(
                        WalletModeSelectionNavigationEvent.PhraseRecovery(
                            walletActivationRequired = modelState.shouldActivateWalletForMode(intent.walletMode)
                        )
                    )
                } else {
                    updateActiveWalletMode(intent.walletMode)
                }
            }

            WalletModeSelectionIntent.DeFiOnboardingComplete -> {
                updateActiveWalletMode(WalletMode.NON_CUSTODIAL_ONLY)
            }
        }.exhaustive
    }

    private fun updateActiveWalletMode(walletMode: WalletMode) {
        updateState {
            it.copy(enabledWalletMode = walletMode)
        }
        walletModeService.updateEnabledWalletMode(walletMode)

        navigate(WalletModeSelectionNavigationEvent.Close(walletMode = walletMode))
    }

    private fun WalletModeSelectionModelState.shouldBackupPhraseForMode(walletMode: WalletMode): Boolean {
        return when (walletMode) {
            WalletMode.NON_CUSTODIAL_ONLY -> isWalletBackedUp.not() && isWalletBackUpSkipped.not()
            else -> false
        }
    }

    private fun WalletModeSelectionModelState.shouldActivateWalletForMode(walletMode: WalletMode): Boolean {
        val isWalletEligibleForActivation = when (walletMode) {
            WalletMode.NON_CUSTODIAL_ONLY -> defiBalance?.isZero == true
            else -> false
        }
        return isWalletEligibleForActivation && shouldBackupPhraseForMode(walletMode)
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
    val enabledWalletMode: WalletMode,
) : ViewState

data class WalletModeSelectionModelState(
    val isWalletBackedUp: Boolean,
    val isWalletBackUpSkipped: Boolean,
    val brokerageBalance: Money?,
    val anyBrokerageBalanceFailed: Boolean,
    val defiBalance: Money?,
    val anyDefiBalanceFailed: Boolean,
    val enabledWalletMode: WalletMode,
) : ModelState

sealed class BalanceState {
    object Loading : BalanceState()
    data class Data(val money: Money) : BalanceState()
    object ActivationRequired : BalanceState()
}

sealed interface WalletModeSelectionNavigationEvent : NavigationEvent {
    data class PhraseRecovery(val walletActivationRequired: Boolean) : WalletModeSelectionNavigationEvent
    data class Close(val walletMode: WalletMode) : WalletModeSelectionNavigationEvent
}

@StringRes
fun WalletMode.title(): Int = when (this) {
    WalletMode.NON_CUSTODIAL_ONLY -> R.string.defi_wallet_name
    WalletMode.CUSTODIAL_ONLY -> R.string.brokerage_wallet_name
    else -> throw IllegalArgumentException("No title supported for mode")
}

@DrawableRes
fun WalletMode.icon(): Int = when (this) {
    WalletMode.NON_CUSTODIAL_ONLY -> R.drawable.ic_defi_wallet
    WalletMode.CUSTODIAL_ONLY -> R.drawable.ic_portfolio
    else -> throw IllegalArgumentException("No icon supported for mode")
}
