package com.blockchain.earn.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.earn.R
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardError
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardNavigationEvent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewModel
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class EarnDashboardFragment :
    MVIFragment<EarnDashboardViewState>(),
    KoinScopeComponent,
    NavigationRouter<EarnDashboardNavigationEvent> {

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<EarnDashboardViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(viewModel, this@EarnDashboardFragment, ModelConfigArgs.NoArgs)

                EarnDashboardScreen(viewModel)
            }
        }
    }

    override fun onStateUpdated(state: EarnDashboardViewState) {
    }

    override fun route(navigationEvent: EarnDashboardNavigationEvent) {
    }

    companion object {
        fun newInstance() = EarnDashboardFragment()
    }
}

@Composable
fun EarnDashboardScreen(
    viewModel: EarnDashboardViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: EarnDashboardViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        when {
            state.isLoading -> EarnDashboardLoading()
            state.needsToCompleteKyc -> {
            }
            state.earnDashboardError != EarnDashboardError.None -> {
            }
            else -> EarnDashboard(state)
        }
    }
}

@Composable
fun EarnDashboard(state: EarnDashboardViewState) {
}

@Composable
fun EarnDashboardLoading() {
    Column(modifier = Modifier.padding(dimensionResource(R.dimen.standard_spacing))) {
        ShimmerLoadingTableRow(false)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(false)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))
    }
}
