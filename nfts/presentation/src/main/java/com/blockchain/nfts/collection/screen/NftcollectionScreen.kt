package com.blockchain.nfts.collection.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.nfts.collection.NftCollectionViewModel
import com.blockchain.nfts.collection.NftCollectionViewState
import com.blockchain.nfts.comingsoon.screen.NftComingSoonScreen

@Composable
fun NftCollection(viewModel: NftCollectionViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: NftCollectionViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        NftComingSoonScreen()
    }
}

@Composable
fun NftCollectionScreen() {
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen() {
    NftCollectionScreen()
}
