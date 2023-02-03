package com.blockchain.nfts.detail.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.expandables.ExpandableItemBordered
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.media.UrlType
import com.blockchain.componentlib.sheets.SheetNub
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.UltraLight
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.R
import com.blockchain.nfts.collection.NftCollectionIntent
import com.blockchain.nfts.detail.NftDetailIntent
import com.blockchain.nfts.detail.NftDetailViewModel
import com.blockchain.nfts.detail.NftDetailViewState
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator
import com.blockchain.nfts.domain.models.NftTrait
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

private const val COLUMN_COUNT = 2

@Composable
fun NftDetail(
    nftId: String,
    address: String,
    pageKey: String?,
    viewModel: NftDetailViewModel = getViewModel(
        scope = payloadScope,
        key = nftId,
        parameters = { parametersOf(nftId, address, pageKey) }
    )
) {
    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(NftDetailIntent.LoadData)
        onDispose { }
    }

    val viewState: NftDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    NftDetailScreen(
        viewState.nftAsset,
        onExternalViewClick = { nftAsset ->
            viewModel.onIntent(NftDetailIntent.ExternalViewRequested(nftAsset))
        }
    )
}

@Composable
fun NftDetailScreen(
    nftAsset: DataResource<NftAsset?>,
    onExternalViewClick: (NftAsset) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SheetNub(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))
        }

        (nftAsset as? DataResource.Data)?.data?.let {
            NftDetailDataScreen(
                nftAsset = it,
                onExternalViewClick = { onExternalViewClick(it) }
            )
        } ?: kotlin.run {
            Text(
                text = nftAsset::class.java.toString(),
                style = AppTheme.typography.title2,
                color = AppTheme.colors.title
            )
        }
    }
}

@Composable
fun NftDetailDataScreen(
    nftAsset: NftAsset,
    onExternalViewClick: () -> Unit
) {

    LazyVerticalGrid(
        modifier = Modifier.fillMaxWidth(),
        columns = GridCells.Fixed(count = COLUMN_COUNT),
        contentPadding = PaddingValues(
            start = AppTheme.dimensions.smallSpacing,
            top = AppTheme.dimensions.verySmallSpacing,
            end = AppTheme.dimensions.smallSpacing,
            bottom = AppTheme.dimensions.standardSpacing
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing)
    ) {
        item(span = { GridItemSpan(COLUMN_COUNT) }) {
            NftBasicInfo(
                nftAsset = nftAsset,
                onExternalViewClick = onExternalViewClick
            )
        }

        items(
            items = nftAsset.traits,
            itemContent = { trait ->
                NftTrait(trait)
            }
        )
    }
}

@Composable
fun NftBasicInfo(
    nftAsset: NftAsset,
    onExternalViewClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp),
            elevation = 0.dp,
            backgroundColor = Color.White,
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
        ) {
            AsyncMediaItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)),
                url = nftAsset.imageUrl,
                fallbackUrlType = UrlType.GIF,
                contentScale = ContentScale.FillWidth,
                onLoadingPlaceholder = 0,
                onErrorDrawable = 0
            )
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        MinimalButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.nft_cta_view),
            icon = ImageResource.Local(
                R.drawable.ic_external,
                colorFilter = ColorFilter.tint(AppTheme.colors.primary),
                size = AppTheme.dimensions.standardSpacing
            ),
            onClick = onExternalViewClick
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

        Text(
            text = nftAsset.name,
            style = AppTheme.typography.title2,
            color = AppTheme.colors.title
        )

        NftCreator(creator = nftAsset.creator)

        ExpandableItemBordered(
            title = stringResource(R.string.nft_description),
            text = nftAsset.description,
            numLinesVisible = 2,
            textButtonToExpand = stringResource(R.string.coinview_expandable_button),
            textButtonToCollapse = stringResource(R.string.coinview_collapsable_button)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
    }
}

@Composable
fun NftCreator(creator: NftCreator) {
    val verifiedIconPadding = 2.5.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(
                AppTheme.dimensions.hugeSpacing + verifiedIconPadding // 2.5 extra to account for verified icon
            )
        ) {
            Image(
                imageResource = ImageResource.Remote(
                    url = creator.imageUrl,
                    shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiSmall),
                    size = 40.dp
                )
            )

            if (creator.isVerified) {
                Image(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    imageResource = ImageResource.Local(R.drawable.ic_verified)
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Column(modifier = Modifier.padding(bottom = verifiedIconPadding)) { // 2.5 extra to account for verified icon
            Text(
                text = creator.name,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

            Text(
                text = stringResource(R.string.nft_creator),
                style = AppTheme.typography.caption1,
                color = Grey700
            )
        }
    }
}

@Composable
fun NftTrait(trait: NftTrait) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = UltraLight,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiSmall)
            )
            .border(
                width = AppTheme.dimensions.borderSmall,
                color = AppTheme.colors.medium,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiSmall)
            )
            .padding(
                start = AppTheme.dimensions.tinySpacing,
                end = AppTheme.dimensions.tinySpacing,
                top = AppTheme.dimensions.tinySpacing,
                bottom = AppTheme.dimensions.smallSpacing,
            )
    ) {
        Text(
            text = trait.name,
            style = AppTheme.typography.caption1,
            color = AppTheme.colors.primaryMuted
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            text = trait.value,
            style = AppTheme.typography.paragraph1,
            color = AppTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Data() {
    NftDetailScreen(
        nftAsset = DataResource.Data(
            NftAsset(
                id = "",
                pageKey = "",
                tokenId = "",
                imageUrl = "",
                name = "Kyotoangels #8260",
                description = "Kyoto Angels is a Collection of 10000 Kawaii Dolls Manufactured by UwU",
                contract = NftContract(address = ""),
                creator = NftCreator(
                    imageUrl = "",
                    name = "Kyotoangels",
                    isVerified = true
                ),
                traits = listOf()
            )
        ),
        {}
    )
}
