package com.dex.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.CloseIcon
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.dex.presentation.R
import com.blockchain.preferences.DexPrefs
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import org.koin.androidx.compose.get

private data class DexIntroItem(
    val icon: Int,
    val title: Int,
    val subtitle: Int
)

private val items = listOf(
    DexIntroItem(
        icon = R.drawable.ic_dex_intro_0,
        title = com.blockchain.stringResources.R.string.welcome_to_dex,
        subtitle = com.blockchain.stringResources.R.string.welcome_to_dex_subtitle
    ),
    DexIntroItem(
        icon = R.drawable.ic_dex_intro_1,
        title = com.blockchain.stringResources.R.string.swap_1000_tokens,
        subtitle = com.blockchain.stringResources.R.string.swap_1000_tokens_subtitle
    ),
    DexIntroItem(
        icon = R.drawable.ic_dex_intro_2,
        title = com.blockchain.stringResources.R.string.keep_control_of_funds,
        subtitle = com.blockchain.stringResources.R.string.keep_control_of_funds_subtitle
    )
)

@Composable
fun DexIntroduction(
    close: () -> Unit,
    dexIntroPrefs: DexPrefs = get(),
    analytics: Analytics = get(),
) {
    LaunchedEffect(key1 = Unit) {
        dexIntroPrefs.markDexIntroAsSeen()
        analytics.logEvent(DexAnalyticsEvents.OnboardingViewed)
    }

    DexIntroductionScreen(
        close = close
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun DexIntroductionScreen(
    close: () -> Unit,
) {
    val pagerState = rememberPagerState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            count = items.size,
            state = pagerState
        ) { pageIndex ->
            DexIntroductionPage(items[pageIndex])
        }

        CloseIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppTheme.dimensions.standardSpacing),
            isScreenBackgroundSecondary = false,
            onClick = close
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.smallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPagerIndicator(
                modifier = Modifier.padding(AppTheme.dimensions.smallestSpacing),
                pagerState = pagerState,
                activeColor = AppTheme.colors.primary,
                inactiveColor = Grey100
            )

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.dimensions.smallSpacing),
                onClick = { close() },
                text = stringResource(id = com.blockchain.stringResources.R.string.start_trading)
            )
        }
    }
}

@Composable
private fun DexIntroductionPage(item: DexIntroItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.standardSpacing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
                .weight(0.8F),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageResource = ImageResource.Local(item.icon)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = item.title),
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Text(
                text = stringResource(id = item.subtitle),
                style = AppTheme.typography.paragraph1,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        }
    }
}

@Preview
@Composable
private fun PreviewDexIntroductionScreen() {
    DexIntroductionScreen(
        close = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDexIntroductionScreenDark() {
    PreviewDexIntroductionScreen()
}
