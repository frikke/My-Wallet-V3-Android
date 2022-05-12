package com.blockchain.commonarch.presentation.mvi_v2

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs.ParcelableArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <TViewState : ViewState, TFragment : MVIBottomSheet<TViewState>, TArgs : ParcelableArgs> TFragment.withArgs(
    key: String,
    args: TArgs
): TFragment = apply {
    arguments = Bundle().apply {
        putParcelable(key, args)
    }
}

abstract class MVIBottomSheet<TViewState : ViewState> : BottomSheetDialogFragment() {

    abstract fun onStateUpdated(state: TViewState)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setCanceledOnTouchOutside(false)

            setOnShowListener {
                behavior.apply {
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }
}

fun <TViewState : ViewState> MVIBottomSheet<TViewState>.disableDragging() {
    (dialog as BottomSheetDialog).apply {
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
    }
}

fun <TIntent : Intent<TModelState>,
    TViewState : ViewState,
    TModelState : ModelState,
    NavEnt : NavigationEvent,
    TArgs : ModelConfigArgs
    > MVIBottomSheet<TViewState>.bindViewModel(
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
