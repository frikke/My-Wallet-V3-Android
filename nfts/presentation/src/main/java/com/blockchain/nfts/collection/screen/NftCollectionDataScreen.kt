package com.blockchain.nfts.collection.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.lazylist.PaginatedLazyVerticalGrid
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.media.UrlType
import com.blockchain.componentlib.swiperefresh.SwipeRefreshWithoutOverscroll
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.nfts.R
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

private const val COLUMN_COUNT = 2

@Composable
fun NftCollectionDataScreen(
    collection: List<NftAsset>,
    isRefreshing: Boolean,
    onItemClick: (NftAsset) -> Unit,
    onExternalShopClick: () -> Unit,
    onRefresh: () -> Unit,
    onGetNextPage: () -> Unit
) {
    SwipeRefreshWithoutOverscroll(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            PaginatedLazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.smallSpacing
                ),
                columns = GridCells.Fixed(count = COLUMN_COUNT),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
                onGetNextPage = onGetNextPage,
                loadNextPageItemOffset = 4
            ) {
                items(
                    items = collection,
                    itemContent = { nftAsset ->
                        AsyncMediaItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1F)
                                .clip(RoundedCornerShape(size = AppTheme.dimensions.borderRadiiSmall))
                                .clickableNoEffect { onItemClick(nftAsset) },
                            url = nftAsset.imageUrl,
                            fallbackUrlType = UrlType.GIF
                        )
                    }
                )

                item(span = { GridItemSpan(COLUMN_COUNT) }) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.xHugeSpacing))
                }
            }

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.tinySpacing)
                    .align(Alignment.BottomCenter),
                text = stringResource(R.string.nft_cta_shop),
                icon = ImageResource.Local(
                    R.drawable.ic_external,
                    colorFilter = ColorFilter.tint(AppTheme.colors.background),
                    size = AppTheme.dimensions.standardSpacing
                ),
                onClick = onExternalShopClick
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionDataScreen() {
    NftCollectionDataScreen(
        collection = listOf(
            NftAsset(
                id = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            ),
            NftAsset(
                id = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            ),
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
        ),
        isRefreshing = false,
        onItemClick = {},
        onExternalShopClick = {},
        onRefresh = {},
        onGetNextPage = {}
    )
}
