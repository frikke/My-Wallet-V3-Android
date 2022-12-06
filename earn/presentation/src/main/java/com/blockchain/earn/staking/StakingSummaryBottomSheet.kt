package com.blockchain.earn.staking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.earn.staking.viewmodel.StakingSummaryArgs
import com.blockchain.earn.staking.viewmodel.StakingSummaryNavigationEvent
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewModel
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewState
import com.blockchain.koin.payloadScope
import info.blockchain.balance.Currency
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class StakingSummaryBottomSheet :
    MVIBottomSheet<StakingSummaryViewState>(),
    KoinScopeComponent,
    NavigationRouter<StakingSummaryNavigationEvent> {

    interface Host : MVIBottomSheet.Host {
        fun openExternalUrl(url: String)
        fun launchStakingWithdrawal(currency: Currency)
        fun launchStakingDeposit(currency: Currency)
        fun showStakingLoadingError(error: StakingError)
        fun goToStakingAccountActivity(currency: Currency)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a StakingSummaryBottomSheet.Host"
        )
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<StakingSummaryViewModel>()

    private val cryptoTicker by lazy {
        arguments?.getString(ASSET_TICKER) ?: throw IllegalStateException(
            "StakingSummaryBottomSheet requires a ticker to start"
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(
                    viewModel = viewModel,
                    navigator = this@StakingSummaryBottomSheet,
                    args = StakingSummaryArgs(cryptoTicker)
                )

                StakingSummaryScreen(
                    viewModel = viewModel,
                    onClosePressed = this@StakingSummaryBottomSheet::dismiss,
                    onLoadError = { error ->
                        dismiss()
                        host.showStakingLoadingError(error)
                    },
                    onWithdrawPressed = { currency ->
                        dismiss()
                        host.launchStakingWithdrawal(currency)
                    },
                    onDepositPressed = { currency ->
                        dismiss()
                        host.launchStakingDeposit(currency)
                    },
                    withdrawDisabledLearnMore = {
                        dismiss()
                        host.openExternalUrl(ETH_STAKING_CONSIDERATIONS)
                    },
                    onViewActivityPressed = { currency ->
                        dismiss()
                        host.goToStakingAccountActivity(currency)
                    }
                )
            }
        }
    }

    override fun onStateUpdated(state: StakingSummaryViewState) {
    }

    override fun route(navigationEvent: StakingSummaryNavigationEvent) {
    }

    companion object {
        private const val ASSET_TICKER = "ASSET_TICKER"
        private const val ETH_STAKING_CONSIDERATIONS = "https://ethereum.org/staking/"

        fun newInstance(cryptoTicker: String) = StakingSummaryBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ASSET_TICKER, cryptoTicker)
            }
        }
    }
}

@Composable
fun StakingSummaryScreen(
    viewModel: StakingSummaryViewModel,
    onClosePressed: () -> Unit,
    onLoadError: (StakingError) -> Unit,
    onWithdrawPressed: (currency: Currency) -> Unit,
    onDepositPressed: (currency: Currency) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onViewActivityPressed: (currency: Currency) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: StakingSummaryViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        Column {
            when {
                state.isLoading -> {
                    SummarySheetLoading()
                }
                state.errorState != StakingError.None -> {
                    onLoadError(state.errorState)
                }
                else -> {
                    StakingSummarySheet(
                        state = state,
                        onWithdrawPressed = onWithdrawPressed,
                        onDepositPressed = onDepositPressed,
                        withdrawDisabledLearnMore = withdrawDisabledLearnMore,
                        onClosePressed = onClosePressed,
                        onViewActivityPressed = onViewActivityPressed
                    )
                }
            }
        }
    }
}
