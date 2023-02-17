package com.blockchain.earn.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.EpicVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.earn.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalPagerApi::class)
@Composable
fun EarnProductOnboarding(
    onboardingPages: List<EarnOnboardingProductPage>,
    onFinishOnboarding: () -> Unit
) {

    val configuration = LocalConfiguration.current
    configuration.screenHeightDp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(configuration.screenHeightDp.dp)
            .padding(AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val pagerState = rememberPagerState()

        HorizontalPager(
            count = onboardingPages.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            Column(modifier = Modifier.fillMaxWidth()) {

                val onboardingPage = onboardingPages[pageIndex]

                Image(imageResource = onboardingPage.image, modifier = Modifier.fillMaxWidth())

                StandardVerticalSpacer()

                SimpleText(
                    text = onboardingPage.title,
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier.fillMaxWidth()
                )

                SmallVerticalSpacer()

                SimpleText(
                    text = onboardingPage.subtitle,
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier.fillMaxWidth()
                )

                EpicVerticalSpacer()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // TODO(labreu): hardcoded to avoid overlap with bottom nav,
                // this screen will become a bottomsheet in the future
                .padding(bottom = 150.dp)
        ) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(AppTheme.dimensions.tinySpacing),
                inactiveColor = AppTheme.colors.medium,
                activeColor = AppTheme.colors.primary
            )

            SmallVerticalSpacer()

            PrimaryButton(
                text = stringResource(R.string.earn_onboarding_cta),
                onClick = onFinishOnboarding,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EarnProductOnboardingPreview() {
    EarnProductOnboarding(
        onboardingPages = listOf(
            EarnOnboardingProductPage.Intro,
            EarnOnboardingProductPage.Interest,
            EarnOnboardingProductPage.Staking,
            EarnOnboardingProductPage.ActiveRewards
        ),
        onFinishOnboarding = {}
    )
}

enum class EarnOnboardingProductPage {
    Intro,
    Interest,
    Staking,
    ActiveRewards;

    val title: String
        @Composable
        get() = when (this) {
            Intro -> stringResource(R.string.earn_onboarding_intro_title)
            Interest -> stringResource(id = R.string.earn_onboarding_passive_title)
            Staking -> stringResource(id = R.string.earn_onboarding_staking_title)
            ActiveRewards -> stringResource(id = R.string.earn_onboarding_active_title)
        }

    val subtitle: String
        @Composable
        get() = when (this) {
            Intro -> stringResource(R.string.earn_onboarding_intro_subtitle)
            Interest -> stringResource(id = R.string.earn_onboarding_passive_subtitle)
            Staking -> stringResource(id = R.string.earn_onboarding_staking_subtitle)
            ActiveRewards -> stringResource(id = R.string.earn_onboarding_active_subtitle)
        }

    val image: ImageResource
        get() = when (this) {
            Intro -> ImageResource.Local(R.drawable.earn_onboarding_intro)
            Interest -> ImageResource.Local(R.drawable.earn_onboarding_passive)
            Staking -> ImageResource.Local(R.drawable.earn_onboarding_staking)
            ActiveRewards -> ImageResource.Local(R.drawable.earn_onboarding_active)
        }
}
