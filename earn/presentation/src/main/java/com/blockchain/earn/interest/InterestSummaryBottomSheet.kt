package com.blockchain.earn.interest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.earn.common.EarnFieldExplainer
import com.blockchain.earn.common.EarnFieldExplainerBottomSheet
import com.blockchain.earn.interest.viewmodel.InterestError
import com.blockchain.earn.interest.viewmodel.InterestSummaryArgs
import com.blockchain.earn.interest.viewmodel.InterestSummaryNavigationEvent
import com.blockchain.earn.interest.viewmodel.InterestSummaryViewModel
import com.blockchain.earn.interest.viewmodel.InterestSummaryViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class InterestSummaryBottomSheet :
    MVIBottomSheet<InterestSummaryViewState>(),
    KoinScopeComponent,
    NavigationRouter<InterestSummaryNavigationEvent> {

    interface Host : MVIBottomSheet.Host {
        fun openExternalUrl(url: String)
        fun launchInterestDeposit(account: EarnRewardsAccount.Interest)
        fun launchInterestWithdrawal(sourceAccount: BlockchainAccount)
        fun showInterestLoadingError(error: InterestError)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a InterestSummaryBottomSheet.Host"
        )
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<InterestSummaryViewModel>()

    private val cryptoTicker by lazy {
        arguments?.getString(ASSET_TICKER) ?: throw IllegalStateException(
            "InterestSummaryBottomSheet requires a ticker to start"
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(
                    viewModel = viewModel,
                    navigator = this@InterestSummaryBottomSheet,
                    args = InterestSummaryArgs(cryptoTicker)
                )

                InterestSummaryScreen(
                    viewModel = viewModel,
                    onClosePressed = this@InterestSummaryBottomSheet::dismiss,
                    onLoadError = { error ->
                        dismiss()
                        host.showInterestLoadingError(error)
                    },
                    onWithdrawPressed = { account ->
                        dismiss()
                        host.launchInterestWithdrawal(account)
                    },
                    onDepositPressed = { account ->
                        dismiss()
                        host.launchInterestDeposit(account)
                    },
                    onExplainerClicked = { earnField ->
                        showEarnFieldExplainer(earnField)
                    }
                )
            }
        }
    }

    override fun onStateUpdated(state: InterestSummaryViewState) {
    }

    override fun route(navigationEvent: InterestSummaryNavigationEvent) {
    }

    private fun showEarnFieldExplainer(earnField: EarnFieldExplainer) =
        (activity as BlockchainActivity).showBottomSheet(EarnFieldExplainerBottomSheet.newInstance(earnField))

    companion object {
        private const val ASSET_TICKER = "ASSET_TICKER"

        fun newInstance(cryptoTicker: String) = InterestSummaryBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ASSET_TICKER, cryptoTicker)
            }
        }
    }
}

@Composable
fun InterestSummaryScreen(
    viewModel: InterestSummaryViewModel,
    onClosePressed: () -> Unit,
    onLoadError: (InterestError) -> Unit,
    onWithdrawPressed: (sourceAccount: BlockchainAccount) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Interest) -> Unit,
    onExplainerClicked: (EarnFieldExplainer) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: InterestSummaryViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        Column(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
            when {
                state.isLoading -> {
                    SummarySheetLoading()
                }
                state.errorState != InterestError.None -> {
                    onLoadError(state.errorState)
                }
                else -> {
                    InterestSummarySheet(
                        state = state,
                        onWithdrawPressed = onWithdrawPressed,
                        onDepositPressed = onDepositPressed,
                        onClosePressed = onClosePressed,
                        onExplainerClicked = onExplainerClicked
                    )
                }
            }
        }
    }
}

@Composable
fun SummarySheetLoading() {
    Column {
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
    }
}
