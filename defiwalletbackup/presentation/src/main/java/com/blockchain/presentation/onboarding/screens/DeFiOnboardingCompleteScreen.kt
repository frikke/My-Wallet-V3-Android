package com.blockchain.presentation.onboarding.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.R
import com.blockchain.presentation.onboarding.DeFiOnboardingIntent
import com.blockchain.presentation.onboarding.viewmodel.DeFiOnboardingViewModel

/**
 * Figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=260%3A18073
 */
@Composable
fun DeFiOnboardingComplete(viewModel: DeFiOnboardingViewModel) {
    DeFiOnboardingCompleteScreen(
        continueOnClick = { viewModel.onIntent(DeFiOnboardingIntent.EndFlow(isSuccessful = true)) }
    )
}

@Composable
fun DeFiOnboardingCompleteScreen(
    continueOnClick: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(title = stringResource(R.string.defi_wallet_name))

        Box {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.8f)
                    .align(Alignment.BottomCenter),
                imageResource = ImageResource.Local(R.drawable.ic_grid),
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.dimensions.paddingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1F))

                Image(
                    imageResource = ImageResource.Local(R.drawable.ic_defi_onboarding)
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingLarge))

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        R.string.defi_onboarding_complete_title,
                        stringResource(R.string.defi_wallet_name)
                    ),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.defi_onboarding_complete_description),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                Spacer(modifier = Modifier.weight(2F))

                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.common_continue),
                    onClick = continueOnClick
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
fun PreviewDeFiOnboardingCompleteScreen() {
    DeFiOnboardingCompleteScreen {}
}
