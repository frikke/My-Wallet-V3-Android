package com.blockchain.nfts.collection.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.NewWindow
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.nfts.R

@Composable
fun NftEmptyCollectionScreen(
    gridState: LazyGridState,
    onExternalShopClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(count = 1),
        state = gridState,
        contentPadding = PaddingValues(
            AppTheme.dimensions.smallSpacing
        ),
        content = {
            item {
                NftEmptyCollection(
                    onExternalShopClick = onExternalShopClick,
                    onReceiveClick = onReceiveClick,
                    onHelpClick = onHelpClick
                )
            }
        }
    )
}

@Composable
private fun NftEmptyCollection(
    onExternalShopClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1F))

        Image(imageResource = ImageResource.Local(R.drawable.ic_nft_hero))

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Text(
            text = stringResource(R.string.nft_empty_title),
            style = AppTheme.typography.title3,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            text = stringResource(R.string.nft_empty_description),
            style = AppTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.muted
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Row(modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = stringResource(R.string.nft_cta_buy),
                icon = Icons.NewWindow.withTint(AppTheme.colors.background),
                onClick = onExternalShopClick
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            PrimaryButton(
                modifier = Modifier.weight(1F),
                text = stringResource(R.string.common_receive),
                icon = Icons.Receive.withTint(AppTheme.colors.background),
                onClick = onReceiveClick
            )
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Text(
            modifier = Modifier.clickableNoEffect { onHelpClick() },
            text = stringResource(R.string.nft_help),
            style = AppTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.primary
        )

        Spacer(modifier = Modifier.weight(1F))
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftEmptyCollectionScreen() {
    NftEmptyCollectionScreen(
        gridState = rememberLazyGridState(),
        onExternalShopClick = {},
        onReceiveClick = {},
        onHelpClick = {}
    )
}
