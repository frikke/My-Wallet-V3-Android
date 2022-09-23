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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.media.UrlType
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.nfts.R
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator

private const val COLUMN_COUNT = 2

@Composable
fun NftCollectionDataScreen(
    collection: List<NftAsset>,
    onItemClick: (NftAsset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = dimensionResource(R.dimen.small_spacing),
                vertical = dimensionResource(R.dimen.small_spacing)
            ),
            columns = GridCells.Fixed(count = COLUMN_COUNT),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing)
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

            item {
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
            onClick = { /*todo*/ }
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionDataScreen() {
    NftCollectionDataScreen(
        listOf(
            NftAsset(
                id = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract  = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            ),
            NftAsset(
                id = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract  = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            ),
            NftAsset(
                id = "",
                tokenId = "",
                imageUrl = "",
                name = "",
                description = "",
                contract  = NftContract(address = ""),
                creator = NftCreator(imageUrl = "", name = "", isVerified = true),
                traits = listOf()
            )
        ),
        {}
    )
}
