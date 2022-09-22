package com.blockchain.nfts.collection.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.nfts.R
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftData

private const val COLUMN_COUNT = 2

@Composable
fun NftCollectionDataScreen(
    collection: List<NftAsset>
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
            itemsIndexed(
                items = collection,
                itemContent = { index, nftAsset ->
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1F),
                        imageResource = ImageResource.Remote(
                            url = nftAsset.iconUrl,
                            shape = RoundedCornerShape(size = AppTheme.dimensions.borderRadiiSmall)
                        )
                    )
                }
            )
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.tinySpacing)
                .align(Alignment.BottomCenter),
            text = stringResource(R.string.nft_cta_shop),
            //                icon = ImageResource.Local(
            //                    data.start.logo.value,
            //                    colorFilter = ColorFilter.tint(AppTheme.colors.background),
            //                    size = AppTheme.dimensions.standardSpacing
            //                ),
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
                "",
                "https://lh3.googleusercontent.com/DWlQUXP_Y3obWxNTxfj3bBg2COuSONsa36DCiBpo5-8wvd5FpCcSg3ZRWILS1tvcAq7SwyQY-fC6wpkr2lJWDtzM1LhJnbi_NpCLng",
                NftData("", "", listOf())
            ),
            NftAsset(
                "",
                "https://lh3.googleusercontent.com/DWlQUXP_Y3obWxNTxfj3bBg2COuSONsa36DCiBpo5-8wvd5FpCcSg3ZRWILS1tvcAq7SwyQY-fC6wpkr2lJWDtzM1LhJnbi_NpCLng",
                NftData("", "", listOf())
            ),
            NftAsset(
                "",
                "https://lh3.googleusercontent.com/DWlQUXP_Y3obWxNTxfj3bBg2COuSONsa36DCiBpo5-8wvd5FpCcSg3ZRWILS1tvcAq7SwyQY-fC6wpkr2lJWDtzM1LhJnbi_NpCLng",
                NftData("", "", listOf())
            )
        )
    )
}
