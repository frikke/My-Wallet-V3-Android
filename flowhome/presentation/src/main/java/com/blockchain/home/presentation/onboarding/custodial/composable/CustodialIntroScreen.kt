package com.blockchain.home.presentation.onboarding.custodial.composable

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Bank
import com.blockchain.componentlib.icons.Cart
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Interest
import com.blockchain.componentlib.system.ClippedShadow
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tag.Tag
import com.blockchain.componentlib.tag.TagSize
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.home.presentation.onboarding.custodial.CustodialIntroIntent
import com.blockchain.home.presentation.onboarding.custodial.CustodialIntroViewModel
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R.string
import org.koin.androidx.compose.getViewModel

@Composable
fun CustodialIntroScreen(
    viewModel: CustodialIntroViewModel = getViewModel(scope = payloadScope),
    getStartedOnClick: () -> Unit,
) {

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(CustodialIntroIntent.LoadData)
        viewModel.markAsSeen()
        onDispose { }
    }

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    CustodialIntro(
        onGetStartedClicked = {
            getStartedOnClick()
        },
        isEligibleForEarnData = viewState.isEligibleForEarn
    )
}

@Composable
fun CustodialIntro(onGetStartedClicked: () -> Unit, isEligibleForEarnData: DataResource<Boolean>) {
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
            imageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.background_custodial_intro),
            contentScale = ContentScale.FillBounds
        )

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    top = statusBarHeight
                )
        ) {
            StandardVerticalSpacer()

            Tag(
                text = stringResource(id = string.intro_custodial_tag_title),
                size = TagSize.Primary,
                backgroundColor = AppColors.background,
                textColor = AppColors.negative,
                onClick = null
            )

            SmallVerticalSpacer()

            Text(
                text = stringResource(string.custodial_intro_title),
                style = AppTheme.typography.title1,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            SmallVerticalSpacer()

            Text(
                text = stringResource(string.custodial_intro_description), // TODO add bold
                style = AppTheme.typography.body1,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            StandardVerticalSpacer()

            when (isEligibleForEarnData) {
                is DataResource.Loading -> {
                    ShimmerLoadingCard()
                }
                else -> {
                    ClippedShadow(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppTheme.dimensions.mediumElevation,
                        shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing),
                        backgroundColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.90F)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AppTheme.dimensions.smallSpacing,
                                vertical = AppTheme.dimensions.standardSpacing
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(Icons.Cart.withTint(AppTheme.colors.title))

                            SmallHorizontalSpacer()

                            Text(
                                text = stringResource(string.custodial_onboarding_intro_property1_title),
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
                                horizontal = AppTheme.dimensions.smallSpacing,
                                vertical = AppTheme.dimensions.standardSpacing
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(Icons.Bank.withTint(AppTheme.colors.title))

                            SmallHorizontalSpacer()

                            Text(
                                text = stringResource(string.custodial_onboarding_intro_property2_title),
                                style = AppTheme.typography.paragraph2,
                                color = AppTheme.colors.title
                            )
                        }
                    }

                    if (isEligibleForEarnData.dataOrElse(false)) {
                        SmallVerticalSpacer()

                        ClippedShadow(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = AppTheme.dimensions.mediumElevation,
                            shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing),
                            backgroundColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.90F)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = AppTheme.dimensions.smallSpacing,
                                    vertical = AppTheme.dimensions.standardSpacing
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(Icons.Interest.withTint(AppTheme.colors.title))

                                SmallHorizontalSpacer()

                                Text(
                                    text = stringResource(string.custodial_onboarding_intro_property3_title),
                                    style = AppTheme.typography.paragraph2,
                                    color = AppTheme.colors.title
                                )
                            }
                        }
                    }
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
                text = stringResource(string.custodial_intro_defi_disclaimer),
                style = AppTheme.typography.caption1,
                color = AppTheme.colors.body,
                textAlign = TextAlign.Center
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
fun PreviewCustodialOnboardingScreen() {
    CustodialIntro(onGetStartedClicked = {}, DataResource.Data(true))
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCustodialOnboardingScreenDark() {
    PreviewCustodialOnboardingScreen()
}
