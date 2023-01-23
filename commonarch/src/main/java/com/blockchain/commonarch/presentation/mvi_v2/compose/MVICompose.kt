package com.blockchain.commonarch.presentation.mvi_v2.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.rememberFlow
import kotlinx.coroutines.cancel

@Composable
fun <VS : ViewState, N : NavigationEvent, A : ModelConfigArgs> bindViewModel(
    viewModel: MviViewModel<*, VS, *, N, A>,
    args: A,
    route: (N) -> Unit,
) {
    val activity = LocalContext.current.getActivity()
    if ((activity as? BlockchainActivity)?.processDeathOccurredAndThisIsNotLauncherActivity == true) {
        viewModel.viewModelScope.cancel()
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.viewCreated(args)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navigationFlow = rememberFlow(viewModel.navigationEventFlow)
    LaunchedEffect(Unit) {
        navigationFlow.collect {
            route(it)
        }
    }
}

private fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
