package com.blockchain.home.introduction.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Purple0000

@Composable
fun IntroductionScreen(content: IntroductionScreenContent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.standardSpacing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
                .weight(1F),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if(content.isLogo){
                Surface(
                    elevation = 8.dp,
                    shape = AppTheme.shapes.veryLarge,
                    color = AppTheme.colors.background
                ) {
                    Image(
                        modifier = Modifier.padding(AppTheme.dimensions.verySmallSpacing),
                        imageResource = ImageResource.Local(content.image, size = 56.dp)
                    )
                }
            } else {
                Image(
                    imageResource = ImageResource.Local(content.image)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(content.title),
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Text(
                text = stringResource(content.description),
                style = AppTheme.typography.paragraph1,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            content.tag?.let { (text, color) ->
                EducationalWalletModeSecureTag(
                    text = stringResource(text),
                    color = color
                )
            }
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewEducationalWalletModeIntroScreen() {
    IntroductionScreen(introductionsScreens(true).last())
}
