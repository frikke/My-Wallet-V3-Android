package com.blockchain.home.introduction.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SystemColors
import com.blockchain.home.introduction.IntroScreensViewModel
import com.blockchain.home.presentation.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalPagerApi::class)
@Composable
fun IntroductionScreens(
    viewModel: IntroScreensViewModel = getViewModel(),
    isNewUser: Boolean = true,
    launchApp: () -> Unit
) {
    SystemColors(statusBarDarkContent = true)

    val pagerState = rememberPagerState()
    var buttonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .onEach { pageIndex ->
                if (pageIndex == introductionsScreens(isNewUser = isNewUser).lastIndex) {
                    buttonVisible = true
                }
            }
            .collect()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier.fillMaxSize(),
            imageResource = ImageResource.Local(R.drawable.background_gradient),
            contentScale = ContentScale.FillBounds
        )

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            count = introductionsScreens(isNewUser = isNewUser).size,
            state = pagerState
        ) { pageIndex ->
            IntroductionScreen(introductionsScreens(isNewUser = isNewUser)[pageIndex])
        }

        Image(
            modifier = Modifier
                .clickable {
                    viewModel.markAsSeen()
                    launchApp()
                }
                .align(Alignment.TopEnd)
                .padding(AppTheme.dimensions.standardSpacing),
            imageResource = ImageResource.Local(R.drawable.ic_close_circle)
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
                TertiaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.educational_wallet_mode_cta),
                    onClick = {
                        viewModel.markAsSeen()
                        launchApp()
                    }
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            HorizontalPagerIndicator(
                modifier = Modifier.padding(AppTheme.dimensions.smallestSpacing),
                pagerState = pagerState,
                activeColor = AppTheme.colors.background,
                inactiveColor = AppTheme.colors.background.copy(alpha = 0.25F)
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
    IntroductionScreens {}
}
