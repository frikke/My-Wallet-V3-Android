package com.blockchain.commonarch.presentation.mvi_v2

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class MVIActivity<TViewState : ViewState> : BlockchainActivity() {
    abstract fun onStateUpdated(state: TViewState)
}

fun <
    TIntent : Intent<TModelState>,
    TViewState : ViewState,
    TModelState : ModelState,
    NavEnt : NavigationEvent,
    TArgs : ModelConfigArgs
    > MVIActivity<TViewState>.bindViewModel(
    viewModel: MviViewModel<
        TIntent,
        TViewState,
        TModelState,
        NavEnt,
        TArgs
        >,
    navigator: NavigationRouter<NavEnt>,
    args: TArgs
) {
    if (processDeathOccurredAndThisIsNotLauncherActivity) {
        viewModel.viewModelScope.cancel()
        lifecycleScope.cancel()
        return
    }
    viewModel.viewCreated(args)
    // Create a new coroutine in the lifecycleScope
    lifecycleScope.launch {
        // repeatOnLifecycle launches the block in a new coroutine every time the
        // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
        /**
         * Warning: Preferred collecting flows using the repeatOnLifecycle API instead of collecting inside the
         * launchWhenX APIs. As the latter APIs suspend the coroutine instead of cancelling it when the Lifecycle
         * is STOPPED, upstream flows are kept active in the background,
         * potentially emitting new items and wasting resources.
         */
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Trigger the flow and start listening for values.
            // This happens when lifecycle is STARTED and stops
            // collecting when the lifecycle is STOPPED
            viewModel.viewState.collect {
                onStateUpdated(it)
            }
        }
    }
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.navigationEventFlow.collect {
                navigator.route(it)
            }
        }
    }
}

/**
 * Example for usage with Composables
 * @Composable
 * fun MyComposable(model: ExampleViewModel) {
 * val lifecycleOwner = LocalLifecycleOwner.current
 * val stateFlowLifecycleAware = remember(model.viewState, lifecycleOwner) {
 * model.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
 * }
 * val state by stateFlowLifecycleAware.collectAsState(null)
 * //draw the state.
 * val navigationEventLifecycleAware = remember(model.navigationEventFlow, lifecycleOwner) {
 * model.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
 * }
 * val navigationEvent by navigationEventLifecycleAware.collectAsState(null)
 * //navigate
}*/
