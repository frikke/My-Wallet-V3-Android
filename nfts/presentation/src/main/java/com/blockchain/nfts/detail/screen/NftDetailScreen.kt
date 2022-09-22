package com.blockchain.nfts.detail.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.nfts.R
import com.blockchain.nfts.collection.screen.NftCollectionScreen
import com.blockchain.nfts.detail.NftDetailViewModel
import com.blockchain.nfts.detail.NftDetailViewState
import com.blockchain.nfts.domain.models.NftAsset

@Composable
fun NftDetail(viewModel: NftDetailViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: NftDetailViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        NftDetailScreen(state.asset)
    }
}

@Composable
fun NftDetailScreen(nftAsset: DataResource<NftAsset?>) {
    when (nftAsset) {
        DataResource.Loading -> {
        }

        is DataResource.Error -> {
            nftAsset.error.printStackTrace()
        }

        is DataResource.Data -> {
            nftAsset.data?.let {
                NftDetailDataScreen(nftAsset = it)
            }
        }
    }
}

@Composable
fun NftDetailDataScreen(nftAsset: NftAsset) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1F)
                .shadow(elevation = 16.dp),
            imageResource = ImageResource.Remote(
                url = nftAsset.iconUrl,
                shape = RoundedCornerShape(size = AppTheme.dimensions.borderRadiiSmall)
            )
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        MinimalButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.tinySpacing),
            text = stringResource(R.string.nft_cta_shop),
            //                icon = ImageResource.Local(
            //                    data.start.logo.value,
            //                    colorFilter = ColorFilter.tint(AppTheme.colors.background),
            //                    size = AppTheme.dimensions.standardSpacing
            //                ),
            onClick = { /*todo*/ }
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

        Text(
            text = stringResource(R.string.nft_empty_title),
            style = AppTheme.typography.title2,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.title
        )


    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Empty() {
    //    NftDetailScreen(nftCollection = DataResource.Data())
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Data() {
    //    NftDetailScreen(
    //        nftCollection = DataResource.Data(
    //            listOf(
    //                NftAsset("", "", NftData("", "", listOf()))
    //            )
    //        )
    //    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Loading() {
    NftCollectionScreen(nftCollection = DataResource.Loading, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewNftCollectionScreen_Error() {
    NftCollectionScreen(nftCollection = DataResource.Error(Exception()), {})
}