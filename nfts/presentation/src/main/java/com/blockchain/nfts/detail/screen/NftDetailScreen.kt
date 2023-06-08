package com.blockchain.nfts.detail.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.expandables.ExpandableItem
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.media.UrlType
import com.blockchain.componentlib.sheets.SheetNub
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.R
import com.blockchain.nfts.detail.NftDetailIntent
import com.blockchain.nfts.detail.NftDetailViewModel
import com.blockchain.nfts.detail.NftDetailViewState
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.models.NftCreator
import com.blockchain.nfts.domain.models.NftTrait
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

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
        }
    }
}

@Composable
fun NftDetailDataScreen(
    nftAsset: NftAsset,
    onExternalViewClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = AppTheme.dimensions.smallSpacing,
            top = AppTheme.dimensions.verySmallSpacing,
            end = AppTheme.dimensions.smallSpacing,
            bottom = AppTheme.dimensions.standardSpacing
        )
    ) {
        item {
            NftBasicInfo(
                nftAsset = nftAsset,
                onExternalViewClick = onExternalViewClick
            )
        }

        if (nftAsset.traits.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(com.blockchain.stringResources.R.string.nft_properties),
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.body
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }

            roundedCornersItems(
                items = nftAsset.traits
            ) { trait ->
                NftTrait(trait)
            }
        }
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp),
            elevation = 0.dp,
            color = Color.White,
            shape = AppTheme.shapes.large
        ) {
            AsyncMediaItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.large),
                url = nftAsset.imageUrl,
                fallbackUrlType = UrlType.GIF,
                contentScale = ContentScale.FillWidth,
                onLoadingPlaceholder = 0,
                onErrorDrawable = 0
            )
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        MinimalPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(com.blockchain.stringResources.R.string.nft_cta_view),
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

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        NftCreator(creator = nftAsset.creator)

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

        ExpandableItem(
            title = stringResource(com.blockchain.stringResources.R.string.nft_description),
            text = nftAsset.description,
            numLinesVisible = 2,
            textButtonToExpand = stringResource(com.blockchain.stringResources.R.string.coinview_expandable_button),
            textButtonToCollapse = stringResource(com.blockchain.stringResources.R.string.coinview_collapsable_button)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
    }
}

@Composable
fun NftCreator(creator: NftCreator) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundSecondary, AppTheme.shapes.large)
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomStackedIcon(
            icon = if (true) {
                StackedIcon.SmallTag(
                    main = ImageResource.Remote(creator.imageUrl),
                    tag = ImageResource.Local(R.drawable.ic_verified)
                )
            } else {
                StackedIcon.SingleIcon(ImageResource.Remote(creator.imageUrl))
            },
            iconBackground = AppTheme.colors.backgroundSecondary,
            size = AppTheme.dimensions.standardSpacing,
            iconShape = AppTheme.shapes.medium
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Column {
            Text(
                text = creator.name,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

            Text(
                text = stringResource(com.blockchain.stringResources.R.string.nft_creator),
                style = AppTheme.typography.caption1,
                color = AppTheme.colors.body
            )
        }
    }
}

@Composable
fun NftTrait(trait: NftTrait) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = AppTheme.colors.backgroundSecondary)
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Text(
            text = trait.name,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

        Text(
            text = trait.value,
            style = AppTheme.typography.caption1,
            color = AppTheme.colors.body
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
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
                traits = listOf(
                    NftTrait("name", "value"),
                    NftTrait("name", "value"),
                    NftTrait("name", "value"),
                    NftTrait("name", "value")
                )
            )
        ),
        {}
    )
}
