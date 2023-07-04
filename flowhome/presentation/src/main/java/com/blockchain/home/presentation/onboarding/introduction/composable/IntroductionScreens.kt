package com.blockchain.home.presentation.onboarding.introduction.composable

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.CloseIcon
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.TextAnimatedBrush
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.onboarding.introduction.IntroScreensViewModel
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun IntroductionScreens(
    viewModel: IntroScreensViewModel = getViewModel(),
    walletStatusPrefs: WalletStatusPrefs = get(),
    triggeredBy: WalletMode? = null,
    launchApp: () -> Unit,
    close: () -> Unit
) {
    val setup = triggeredBy?.let {
        IntroductionScreensSetup.ModesOnly(startMode = it)
    } ?: IntroductionScreensSetup.All(isNewUser = walletStatusPrefs.isNewlyCreated)

    IntroductionScreensData(
        setup = setup,
        markAsSeen = { viewModel.markAsSeen() },
        launchApp = launchApp,
        close = close
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun IntroductionScreensData(
    setup: IntroductionScreensSetup,
    markAsSeen: () -> Unit,
    launchApp: () -> Unit,
    close: () -> Unit
) {
    val pagerState = rememberPagerState()
    var buttonVisible by remember { mutableStateOf(false) }
    var swipeHintVisible by remember { mutableStateOf(true) }

    val introductionsScreens = remember {
        introductionsScreens(introductionScreensSetup = setup)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .onEach { pageIndex ->
                if (pageIndex == introductionsScreens.lastIndex) {
                    buttonVisible = true
                }

                if (pageIndex != 0) {
                    swipeHintVisible = false
                }
            }
            .collect()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            imageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.background_gradient),
            contentScale = ContentScale.FillBounds
        )

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            count = introductionsScreens.size,
            state = pagerState
        ) { pageIndex ->
            IntroductionScreen(introductionsScreens[pageIndex])
        }

        CloseIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppTheme.dimensions.standardSpacing),
            isScreenBackgroundSecondary = false,
            onClick = {
                markAsSeen()
                if (setup is IntroductionScreensSetup.All) {
                    launchApp()
                } else {
                    close()
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.smallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MinimalPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(com.blockchain.stringResources.R.string.done),
                    onClick = {
                        markAsSeen()
                        if (setup is IntroductionScreensSetup.All) {
                            launchApp()
                        } else {
                            close()
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = swipeHintVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextAnimatedBrush(
                    text = stringResource(com.blockchain.stringResources.R.string.intro_swipe_hint),
                    style = AppTheme.typography.body2,
                    baseColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.4F),
                    brushColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.9F),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            HorizontalPagerIndicator(
                modifier = Modifier.padding(AppTheme.dimensions.smallestSpacing),
                pagerState = pagerState,
                activeColor = AppTheme.colors.backgroundSecondary,
                inactiveColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.25F)
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewIntroductionScreens() {
    IntroductionScreensData(IntroductionScreensSetup.ModesOnly(WalletMode.CUSTODIAL), {}, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewIntroductionScreensDark() {
    PreviewIntroductionScreens()
}
