package com.blockchain.presentation.onboarding.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White800
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.presentation.R
import com.blockchain.presentation.onboarding.DeFiOnboardingIntent
import com.blockchain.presentation.onboarding.OnboardingAnalyticsEvents
import com.blockchain.presentation.onboarding.viewmodel.DeFiOnboardingViewModel
import org.koin.androidx.compose.get

private data class DefiOnboardingSection(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val image: Int,
    val imageFillWidth: Boolean,
    val isTopSection: Boolean
)

private val sections = listOf(
    DefiOnboardingSection(
        title = R.string.defi_onboarding_intro_section1_title,
        description = R.string.defi_onboarding_intro_section1_description,
        image = R.drawable.defi_intro_phones,
        imageFillWidth = false,
        isTopSection = true
    ),
    DefiOnboardingSection(
        title = R.string.defi_onboarding_intro_section2_title,
        description = R.string.defi_onboarding_intro_section2_description,
        image = R.drawable.defi_intro_wallets,
        imageFillWidth = true,
        isTopSection = false
    ),
    DefiOnboardingSection(
        title = R.string.defi_onboarding_intro_section3_title,
        description = R.string.defi_onboarding_intro_section3_description,
        image = R.drawable.defi_intro_coins,
        imageFillWidth = false,
        isTopSection = false
    ),
    DefiOnboardingSection(
        title = R.string.defi_onboarding_intro_section4_title,
        description = R.string.defi_onboarding_intro_section4_description,
        image = R.drawable.defi_intro_wallet_connect,
        imageFillWidth = true,
        isTopSection = false
    )
)

@Composable
fun DeFiOnboardingIntro(
    analytics: Analytics = get(),
    viewModel: DeFiOnboardingViewModel
) {
    DisposableEffect(Unit) {
        analytics.logEvent(OnboardingAnalyticsEvents.OnboardingViewed)
        onDispose { }
    }

    DeFiOnboardingIntroScreen(
        onContinueClick = {
            viewModel.onIntent(DeFiOnboardingIntent.EnableDeFiWallet)
            analytics.logEvent(OnboardingAnalyticsEvents.OnboardingContinueClicked)
        }
    )
}

@Composable
fun DeFiOnboardingIntroScreen(
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppTheme.colors.backgroundMuted),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                imageResource = ImageResource.Local(R.drawable.onboarding_sheet),
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.epicSpacing))

                sections.forEach { section ->
                    DefiOnboardingSection(data = section)
                }

                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.standardSpacing),
                    text = stringResource(R.string.common_continue),
                    onClick = onContinueClick
                )
            }

            Box(
                modifier = Modifier
                    .padding(AppTheme.dimensions.standardSpacing)
                    .align(Alignment.TopEnd)
                    .clickableNoEffect(onContinueClick)
            ) {
                Image(
                    imageResource = Icons.Close.withBackground(
                        backgroundColor = White800,
                        iconSize = AppTheme.dimensions.standardSpacing,
                        backgroundSize = AppTheme.dimensions.largeSpacing
                    )
                )
            }
        }
    }
}

@Composable
private fun DefiOnboardingSection(
    data: DefiOnboardingSection
) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(data.title),
            style = if (data.isTopSection) AppTheme.typography.title1 else AppTheme.typography.title2,
            color = if (data.isTopSection) AppTheme.colors.background else AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(data.description),
            style = AppTheme.typography.body1,
            color = if (data.isTopSection) AppTheme.colors.background else AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            imageResource = ImageResource.Local(data.image),
            contentScale = if (data.imageFillWidth) ContentScale.FillWidth else ContentScale.Fit
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
    }
}

// ///////////////
// PREVIEWS
// ///////////////
@Preview(showBackground = true)
@Composable
fun PreviewDeFiOnboardingIntroScreen() {
    DeFiOnboardingIntroScreen {}
}
