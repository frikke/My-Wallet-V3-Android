package com.blockchain.home.presentation.onboarding.defi.composable

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.ChartsBubble
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Link
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.system.ClippedShadow
import com.blockchain.componentlib.tag.Tag
import com.blockchain.componentlib.tag.TagSize
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.home.presentation.onboarding.defi.DefiIntroViewModel
import com.blockchain.home.presentation.onboarding.defi.OnboardingAnalyticsEvents
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R.string
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

const val DEFI_LEARN_MORE_URL =
    "https://support.blockchain.com/hc/en-us/articles/360029029911-Blockchain-com-Wallet-101-What-is-a-DeFi-wallet-"

@Composable
fun DefiIntroScreen(
    analytics: Analytics = get(),
    viewModel: DefiIntroViewModel = getViewModel(scope = payloadScope),
    getStartedOnClick: () -> Unit
) {
    DisposableEffect(Unit) {
        viewModel.markAsSeen()
        analytics.logEvent(OnboardingAnalyticsEvents.OnboardingViewed)
        onDispose { }
    }

    val context = LocalContext.current

    DefiIntro(
        onLearnMoreClicked = {
            context.openUrl(DEFI_LEARN_MORE_URL)
        },
        onGetStartedClicked = {
            analytics.logEvent(OnboardingAnalyticsEvents.OnboardingContinueClicked)
            getStartedOnClick()
        }
    )
}

@Composable
fun DefiIntro(onLearnMoreClicked: () -> Unit, onGetStartedClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        val scale = remember { Animatable(1f) }

        // Trigger the animation when the composable is first displayed
        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 2.3f,
                animationSpec = tween(durationMillis = 12000)
            )
        }

        Image(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            imageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.background_defi_intro),
            contentScale = ContentScale.FillBounds
        )

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = AppTheme.dimensions.smallSpacing, end = AppTheme.dimensions.smallSpacing,
                    top = statusBarHeight
                )
        ) {
            StandardVerticalSpacer()

            Tag(
                text = stringResource(id = string.intro_non_custodial_tag_title),
                size = TagSize.Primary,
                backgroundColor = AppColors.background,
                textColor = AppColors.explorer,
                onClick = null
            )

            SmallVerticalSpacer()

            Text(
                text = stringResource(string.defi_intro_title),
                style = AppTheme.typography.title1,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            SmallVerticalSpacer()

            Text(
                text = stringResource(string.defi_intro_description), // TODO add bold
                style = AppTheme.typography.body1,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            StandardVerticalSpacer()

            ClippedShadow(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppTheme.dimensions.mediumElevation,
                shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing),
                backgroundColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.90F)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppTheme.dimensions.smallSpacing, vertical = AppTheme.dimensions.standardSpacing
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(Icons.Lock.withTint(AppTheme.colors.title))

                    SmallHorizontalSpacer()

                    Text(
                        text = stringResource(string.defi_onboarding_intro_property1_title),
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                }
            }

            SmallVerticalSpacer()

            ClippedShadow(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppTheme.dimensions.mediumElevation,
                shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing),
                backgroundColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.90F)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppTheme.dimensions.smallSpacing, vertical = AppTheme.dimensions.standardSpacing
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(Icons.ChartsBubble.withTint(AppTheme.colors.title))

                    SmallHorizontalSpacer()

                    Column {
                        Text(
                            text = stringResource(string.defi_onboarding_intro_property2_title),
                            style = AppTheme.typography.paragraph2,
                            color = AppTheme.colors.title
                        )

                        Image(ImageResource.Local(com.blockchain.componentlib.R.drawable.chain_logos_array))
                    }
                }
            }

            SmallVerticalSpacer()

            ClippedShadow(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppTheme.dimensions.mediumElevation,
                shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing),
                backgroundColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.90F)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppTheme.dimensions.smallSpacing, vertical = AppTheme.dimensions.standardSpacing
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(Icons.Link.withTint(AppTheme.colors.title))

                    SmallHorizontalSpacer()

                    Text(
                        text = stringResource(string.defi_onboarding_intro_property3_title),
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                }
            }

            SmallVerticalSpacer()
        }

        Column(
            modifier = Modifier
                .padding(
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing,
                    top = AppTheme.dimensions.standardSpacing,
                    bottom = navBarHeight
                )
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = stringResource(string.defi_intro_custodial_disclaimer),
                style = AppTheme.typography.caption1,
                color = AppTheme.colors.body,
                textAlign = TextAlign.Center
            )

            SmallVerticalSpacer()

            MinimalPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(min = 56.dp),
                text = stringResource(com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMoreClicked
            )

            SmallVerticalSpacer()

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(min = 56.dp),
                text = stringResource(com.blockchain.stringResources.R.string.common_get_started),
                onClick = onGetStartedClicked
            )

            StandardVerticalSpacer()
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewDefiOnboardingScreen() {
    DefiIntro(onLearnMoreClicked = {}, onGetStartedClicked = {})
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDefiOnboardingScreenDark() {
    PreviewDefiOnboardingScreen()
}
