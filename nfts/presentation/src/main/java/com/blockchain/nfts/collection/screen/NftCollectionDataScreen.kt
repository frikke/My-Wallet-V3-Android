package com.blockchain.nfts.collection.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.lazylist.PaginatedLazyVerticalGrid
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.media.UrlType
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.clickableWithIndication
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.nfts.R
import com.blockchain.nfts.collection.DisplayType
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NftCollectionDataScreen(
    gridState: LazyGridState,
    collection: List<NftAsset>,
    displayType: DisplayType,
    isNextPageLoading: Boolean,
    changeDisplayTypeOnClick: (newDisplayType: DisplayType) -> Unit,
    onItemClick: (NftAsset) -> Unit,
    onGetNextPage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            Text(
                text = stringResource(id = com.blockchain.stringResources.R.string.nft_collectibles),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )

            Spacer(modifier = Modifier.weight(1F))

            if (collection.size > 1) {
                Image(
                    modifier = Modifier.clickableWithIndication {
                        changeDisplayTypeOnClick(displayType.change())
                    },
                    imageResource = displayType.icon
                )
            }
        }

        PaginatedLazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            lazyGridState = gridState,
            contentPadding = PaddingValues(
                AppTheme.dimensions.smallSpacing
            ),
            columns = GridCells.Fixed(count = displayType.columnCount),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
            onGetNextPage = onGetNextPage,
            loadNextPageItemOffset = 6
        ) {
            items(
                items = collection,
                key = { it.id },
                itemContent = { nftAsset ->
                    AsyncMediaItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1F)
                            .clip(RoundedCornerShape(size = AppTheme.dimensions.borderRadiiStandard))
                            .clickableNoEffect { onItemClick(nftAsset) }
                            .animateItemPlacement(),
                        url = nftAsset.imageUrl,
                        contentScale = ContentScale.Crop,
                        fallbackUrlType = UrlType.GIF
                    )
                }
            )

            if (isNextPageLoading) {
                item(span = { GridItemSpan(displayType.columnCount) }) {
                    CircularProgressBar()
                }
            }

            item(span = { GridItemSpan(displayType.columnCount) }) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.xHugeSpacing))
            }
        }
    }
}

private fun DisplayType.change() = when (this) {
    DisplayType.Grid -> DisplayType.List
    DisplayType.List -> DisplayType.Grid
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionDataScreen() {
    var displayType: DisplayType by remember { mutableStateOf(DisplayType.Grid) }

    NftCollectionDataScreen(
        gridState = rememberLazyGridState(),
        collection = listOf(
            NftAsset(
                id = "1",
                pageKey = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            ),
            NftAsset(
                id = "2",
                pageKey = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            ),
            NftAsset(
                id = "3",
                pageKey = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            )
        ),
        displayType = displayType,
        isNextPageLoading = true,
        changeDisplayTypeOnClick = {
            displayType = it
        },
        onItemClick = {},
        onGetNextPage = {}
    )
}
