package com.blockchain.nfts.collection.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.data.DataResource
import com.blockchain.nfts.collection.NftCollectionIntent
import com.blockchain.nfts.collection.NftCollectionViewModel
import com.blockchain.nfts.collection.NftCollectionViewState
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator

@Composable
fun NftCollection(
    viewModel: NftCollectionViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: NftCollectionViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        NftCollectionScreen(
            nftCollection = state.collection,
            onItemClick = { nftAsset ->
                viewModel.onIntent(NftCollectionIntent.ShowDetail(nftId = nftAsset.id))
            },
            onExternalShopClick = {
                viewModel.onIntent(NftCollectionIntent.ExternalShop)
            },
            onReceiveClick = {
                viewModel.onIntent(NftCollectionIntent.ShowReceiveAddress)
            },
            onHelpClick = {
                viewModel.onIntent(NftCollectionIntent.ShowHelp)
            }
        )
    }
}

@Composable
fun NftCollectionScreen(
    nftCollection: DataResource<List<NftAsset>>,
    onItemClick: (NftAsset) -> Unit,
    onExternalShopClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    when (nftCollection) {
        DataResource.Loading -> {
        }

        is DataResource.Error -> {
            nftCollection.error.printStackTrace()
        }

        is DataResource.Data -> {
            with(nftCollection.data) {
                if (isEmpty()) {
                    NftEmptyCollectionScreen(
                        onExternalShopClick = onExternalShopClick,
                        onReceiveClick = onReceiveClick,
                        onHelpClick = onHelpClick
                    )
                } else {
                    NftCollectionDataScreen(
                        collection = this,
                        onItemClick = onItemClick,
                        onExternalShopClick = onExternalShopClick
                    )
                }
            }
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Empty() {
    NftCollectionScreen(
        nftCollection = DataResource.Data(emptyList()),
        onItemClick = {},
        onExternalShopClick = {},
        onReceiveClick = {},
        onHelpClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Data() {
    NftCollectionScreen(
        nftCollection = DataResource.Data(
            listOf(
                NftAsset(
                    id = "",
                    tokenId = "",
                    imageUrl = "",
                    name = "",
                    description = "",
                    contract = NftContract(address = ""),
                    creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                    traits = listOf()
                )
            )
        ),
        onItemClick = {},
        onExternalShopClick = {},
        onReceiveClick = {},
        onHelpClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Loading() {
    NftCollectionScreen(
        nftCollection = DataResource.Loading,
        onItemClick = {},
        onExternalShopClick = {},
        onReceiveClick = {},
        onHelpClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Error() {
    NftCollectionScreen(
        nftCollection = DataResource.Error(Exception()),
        onItemClick = {},
        onExternalShopClick = {},
        onReceiveClick = {},
        onHelpClick = {}
    )
}
