package com.blockchain.nfts.comingsoon.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.nfts.R

@Composable
fun NftComingSoonScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.6F))

        Image(imageResource = ImageResource.Local(R.drawable.ic_nft_hero))

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Text(
            text = stringResource(R.string.nft_coming_soon_title),
            style = AppTheme.typography.title2,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.title,
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            text = stringResource(R.string.nft_coming_soon_description),
            style = AppTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = Grey700,
        )

        Spacer(modifier = Modifier.weight(1F))
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewNftComingSoonScreen() {
    NftComingSoonScreen()
}
