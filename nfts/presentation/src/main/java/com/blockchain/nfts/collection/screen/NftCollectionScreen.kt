package com.blockchain.nfts.collection.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.chrome.LocalNavControllerProvider
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.collection.DisplayType
import com.blockchain.nfts.collection.NftCollectionIntent
import com.blockchain.nfts.collection.NftCollectionViewModel
import com.blockchain.nfts.collection.NftCollectionViewState
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator
import com.blockchain.nfts.navigation.NftDestination
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun NftCollection(
    viewModel: NftCollectionViewModel = getViewModel(scope = payloadScope),
    gridState: LazyGridState,
    shouldTriggerRefresh: Boolean,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    openExternalUrl: (url: String) -> Unit,
    openNftHelp: () -> Unit,
    openNftDetail: (nftId: String, address: String, pageKey: String?) -> Unit
) {
    val navController = LocalNavControllerProvider.current

    val viewState: NftCollectionViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(NftCollectionIntent.LoadData())
        onDispose { }
    }

    DisposableEffect(shouldTriggerRefresh) {
        if (shouldTriggerRefresh) {
            viewModel.onIntent(NftCollectionIntent.Refresh)
        }
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                is NftCollectionNavigationEvent.ShopExternal -> {
                    openExternalUrl(it.url)
                }
                NftCollectionNavigationEvent.ShowHelp -> {
                    openNftHelp()
                }
                is NftCollectionNavigationEvent.ShowReceiveAddress -> {
                    navController.navigate(NftDestination.ReceiveAccountDetail)
//                    nftNavigation.showReceiveSheet(it.account)
                }
                is NftCollectionNavigationEvent.ShowDetail -> {
                    openNftDetail(it.nftId, it.address, it.pageKey)
                }
            }
        }
    }

    NftCollectionScreen(
        gridState = gridState,
        openSettings = openSettings,
        launchQrScanner = launchQrScanner,
        nftCollection = viewState.collection,
        displayType = viewState.displayType,
        isNextPageLoading = viewState.showNextPageLoading,
        changeDisplayTypeOnClick = { newDisplayType ->
            viewModel.onIntent(NftCollectionIntent.ChangeDisplayType(newDisplayType))
        },
        onItemClick = { nftAsset ->
            viewModel.onIntent(
                NftCollectionIntent.ShowDetail(nftId = nftAsset.id, pageKey = nftAsset.pageKey)
            )
        },
        onExternalShopClick = {
            viewModel.onIntent(NftCollectionIntent.ExternalShop)
        },
        onGetNextPage = {
            viewModel.onIntent(NftCollectionIntent.LoadNextPage)
        },
        onReceiveClick = {
            viewModel.onIntent(NftCollectionIntent.ShowReceiveAddress)
        },
        onHelpClick = {
            viewModel.onIntent(NftCollectionIntent.ShowHelp)
        }
    )
}

@Composable
fun NftCollectionScreen(
    gridState: LazyGridState,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    nftCollection: DataResource<List<NftAsset>>,
    displayType: DisplayType,
    isNextPageLoading: Boolean,
    changeDisplayTypeOnClick: (newDisplayType: DisplayType) -> Unit,
    onItemClick: (NftAsset) -> Unit,
    onExternalShopClick: () -> Unit,
    onGetNextPage: () -> Unit,
    onReceiveClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MenuOptionsScreen(
            openSettings = openSettings,
            launchQrScanner = launchQrScanner
        )

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
                            gridState = gridState,
                            onExternalShopClick = onExternalShopClick,
                            onReceiveClick = onReceiveClick,
                            onHelpClick = onHelpClick
                        )
                    } else {
                        NftCollectionDataScreen(
                            gridState = gridState,
                            collection = this,
                            displayType = displayType,
                            isNextPageLoading = isNextPageLoading,
                            changeDisplayTypeOnClick = changeDisplayTypeOnClick,
                            onItemClick = onItemClick,
                            onGetNextPage = onGetNextPage
                        )
                    }
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
        gridState = rememberLazyGridState(),
        nftCollection = DataResource.Data(emptyList()),
        displayType = DisplayType.Grid,
        isNextPageLoading = true,
        onItemClick = {}, onExternalShopClick = {}, onGetNextPage = {}, onReceiveClick = {},
        onHelpClick = {}, openSettings = {}, launchQrScanner = {}, changeDisplayTypeOnClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Data() {
    NftCollectionScreen(
        gridState = rememberLazyGridState(),
        nftCollection = DataResource.Data(
            listOf(
                NftAsset(
                    id = "",
                    pageKey = "",
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
        displayType = DisplayType.Grid,
        isNextPageLoading = true,
        onItemClick = {}, onExternalShopClick = {}, onGetNextPage = {}, onReceiveClick = {},
        onHelpClick = {}, openSettings = {}, launchQrScanner = {}, changeDisplayTypeOnClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Loading() {
    NftCollectionScreen(
        gridState = rememberLazyGridState(),
        nftCollection = DataResource.Loading,
        displayType = DisplayType.Grid,
        isNextPageLoading = true,
        onItemClick = {}, onExternalShopClick = {}, onGetNextPage = {}, onReceiveClick = {},
        onHelpClick = {}, openSettings = {}, launchQrScanner = {}, changeDisplayTypeOnClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Error() {
    NftCollectionScreen(
        gridState = rememberLazyGridState(),
        nftCollection = DataResource.Error(Exception()),
        displayType = DisplayType.Grid,
        isNextPageLoading = true,
        onItemClick = {}, onExternalShopClick = {}, onGetNextPage = {}, onReceiveClick = {},
        onHelpClick = {}, openSettings = {}, launchQrScanner = {}, changeDisplayTypeOnClick = {}
    )
}
