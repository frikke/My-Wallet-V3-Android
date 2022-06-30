package piuk.blockchain.android.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import piuk.blockchain.android.R

class WalletModeSelectionViewModel(
    private val walletModeService: WalletModeService,
    private val cache: WalletModeBalanceCache,
) :
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

                val nonCustodialBalance = cache.stream(
                    KeyedStoreRequest.Cached(
                        key = WalletMode.NON_CUSTODIAL_ONLY,
                        forceRefresh = false
                    )
                ).map { response ->
                    when (response) {
                        is StoreResponse.Data -> updateState {
                            it.copy(
                                defiBalance = response.data.total
                            )
                        }
                        is StoreResponse.Error,
                        StoreResponse.Loading,
                        -> {
                            // Do nothing
                        }
                    }
                }

                val custodialBalance = cache.stream(
                    KeyedStoreRequest.Cached(
                        key = WalletMode.CUSTODIAL_ONLY,
                        forceRefresh = false
                    )
                ).map { response ->
                    when (response) {
                        is StoreResponse.Data -> updateState {
                            it.copy(
                                brokerageBalance = response.data.total
                            )
                        }
                        is StoreResponse.Error,
                        StoreResponse.Loading,
                        -> {
                            // Do nothing
                        }
                    }
                }

                merge(nonCustodialBalance, custodialBalance).collect()
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

@StringRes
fun WalletMode.title(): Int = when (this) {
    WalletMode.NON_CUSTODIAL_ONLY -> R.string.defi_wallet
    WalletMode.CUSTODIAL_ONLY -> R.string.brokerage
    else -> throw IllegalArgumentException("No title supported for mode")
}

@DrawableRes
fun WalletMode.icon(): Int = when (this) {
    WalletMode.NON_CUSTODIAL_ONLY -> R.drawable.ic_defi_wallet
    WalletMode.CUSTODIAL_ONLY -> R.drawable.ic_portfolio
    else -> throw IllegalArgumentException("No icon supported for mode")
}
