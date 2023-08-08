package com.blockchain.commonarch.presentation.mvi_v2

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs.ParcelableArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class MVIFragment<TViewState : ViewState> : Fragment() {
    abstract fun onStateUpdated(state: TViewState)

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment?) =
        bottomSheet?.show(childFragmentManager, BOTTOM_SHEET)

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"
    }
}

fun <TViewState : ViewState, TFragment : MVIFragment<TViewState>, TArgs : ParcelableArgs> TFragment.withArgs(
    key: String,
    args: TArgs
): TFragment = apply {
    arguments = Bundle().apply {
        putParcelable(key, args)
    }
}

fun <
    TIntent : Intent<TModelState>,
    TViewState : ViewState,
    TModelState : ModelState,
    NavEnt : NavigationEvent,
    TArgs : ModelConfigArgs
    > MVIFragment<TViewState>.bindViewModel(
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
    if ((requireActivity() as? BlockchainActivity)?.processDeathOccurredAndThisIsNotLauncherActivity == true) {
        viewModel.viewModelScope.cancel()
        lifecycleScope.cancel()
        try {
            viewLifecycleOwner.lifecycleScope.cancel()
        } catch (ex: Exception) {
            // no-op
        }
        return
    }
    viewModel.viewCreated(args)
    // Create a new coroutine in the lifecycleScope
    viewLifecycleOwner.lifecycleScope.launch {
        // repeatOnLifecycle launches the block in a new coroutine every time the
        // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
        /**
         * Warning: Preferred collecting flows using the repeatOnLifecycle API instead of collecting inside the
         * launchWhenX APIs. As the latter APIs suspend the coroutine instead of cancelling it when the Lifecycle
         * is STOPPED, upstream flows are kept active in the background,
         * potentially emitting new items and wasting resources.
         */
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Trigger the flow and start listening for values.
            // This happens when lifecycle is STARTED and stops
            // collecting when the lifecycle is STOPPED
            viewModel.viewState.collect {
                onStateUpdated(it)
            }
        }
    }
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
