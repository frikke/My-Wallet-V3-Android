package piuk.blockchain.android.ui.dashboard

import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.outcome.doOnSuccess
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class WalletModeSelectionViewModel(private val walletModeService: WalletModeService, private val coincore: Coincore) :
    MviViewModel<
        WalletModeSelectionIntent,
        WalletModeSelectionViewState,
        WalletModeSelectionModelState,
        NavigationEvent,
        ModelConfigArgs.NoArgs>(
        initialState = WalletModeSelectionModelState(
            brokerageBalance = null,
            defiBalance = null,
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
                defiWalletBalance = defiBalance?.let {
                    BalanceState.Data(it)
                } ?: BalanceState.Loading,
                defiWalletAvailable = true,
                enabledWalletMode = enabledWalletMode
            )
        }
    }

    private fun totalBalance(portfolioBalance: Money?, defiBalance: Money?): BalanceState {
        val portfBalance = portfolioBalance ?: return BalanceState.Loading
        val defBalance = defiBalance ?: return BalanceState.Loading
        return BalanceState.Data(portfBalance.plus(defBalance))
    }

    override suspend fun handleIntent(modelState: WalletModeSelectionModelState, intent: WalletModeSelectionIntent) {
        when (intent) {
            WalletModeSelectionIntent.LoadAvailableModesAndBalances -> {
                updateState {
                    it.copy(brokerageBalance = null, defiBalance = null)
                }

                coincore.allWalletsInMode(WalletMode.CUSTODIAL_ONLY).flatMap { it.balance.firstOrError() }
                    .awaitOutcome()
                    .doOnSuccess { balance ->
                        updateState {
                            it.copy(
                                brokerageBalance = balance.total
                            )
                        }
                    }
                coincore.allWalletsInMode(WalletMode.NON_CUSTODIAL_ONLY).flatMap { it.balance.firstOrError() }
                    .awaitOutcome()
                    .doOnSuccess { balance ->
                        updateState {
                            it.copy(
                                defiBalance = balance.total
                            )
                        }
                    }
            }

            is WalletModeSelectionIntent.UpdateActiveWalletMode -> {
                updateState {
                    it.copy(brokerageBalance = null, defiBalance = null, enabledWalletMode = intent.walletMode)
                }
                walletModeService.updateEnabledWalletMode(intent.walletMode)
            }
        }
    }
}

sealed class WalletModeSelectionIntent : Intent<WalletModeSelectionModelState> {
    object LoadAvailableModesAndBalances : WalletModeSelectionIntent()
    data class UpdateActiveWalletMode(val walletMode: WalletMode) : WalletModeSelectionIntent() {
        override fun isValidFor(modelState: WalletModeSelectionModelState): Boolean {
            return modelState.enabledWalletMode != walletMode
        }
    }
}

data class WalletModeSelectionViewState(
    val totalBalance: BalanceState,
    val brokerageBalance: BalanceState,
    val defiWalletBalance: BalanceState,
    val defiWalletAvailable: Boolean,
    val enabledWalletMode: WalletMode,
) : ViewState

data class WalletModeSelectionModelState(
    val brokerageBalance: Money?,
    val defiBalance: Money?,
    val enabledWalletMode: WalletMode,
) : ModelState

sealed class BalanceState {
    object Loading : BalanceState()
    data class Data(val money: Money) : BalanceState()
}
