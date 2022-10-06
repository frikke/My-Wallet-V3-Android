package com.blockchain.home.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.home.presentation.HomeViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = getViewModel(scope = payloadScope),
    listState: LazyListState,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: HomeViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(HomeIntent.LoadHomeAccounts)
        onDispose { }
    }

    viewState?.let { state ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0XFFF1F2F7),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
        ) {
            item {
                HomeAssets(
                    cryptoAssets = state.cryptoAssets,
                    fiatAssets = state.fiatAssets
                )
            }
        }
    }
}
