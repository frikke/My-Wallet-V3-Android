package piuk.blockchain.android.ui.educational.walletmodes.screens

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
import com.blockchain.utils.isLastIn
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import piuk.blockchain.android.R

@OptIn(ExperimentalPagerApi::class)
@Composable
fun EducationalWalletModeScreen(
    getStartedOnClick: () -> Unit,
) {
    val pagerState = rememberPagerState()
    var buttonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .onEach { pageIndex ->
                if (pageIndex isLastIn EducationalWalletModePages.values()) buttonVisible = true
            }
            .collect()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier.fillMaxSize(),
            imageResource = ImageResource.Local(
                R.drawable.background_gradient
            ),
            contentScale = ContentScale.FillBounds
        )

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            count = EducationalWalletModePages.values().size,
            state = pagerState
        ) { pageIndex ->
            EducationalWalletModePage(pageIndex)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (buttonVisible) {
                TertiaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.educational_wallet_mode_cta),
                    onClick = getStartedOnClick
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingMedium))

            HorizontalPagerIndicator(
                modifier = Modifier.padding(AppTheme.dimensions.xPaddingSmall),
                pagerState = pagerState,
                activeColor = AppTheme.colors.background,
                inactiveColor = AppTheme.colors.background.copy(alpha = 0.25F)
            )
        }
    }
}

@Composable
private fun EducationalWalletModePage(index: Int) {
    EducationalWalletModePages.values().first { it.index == index }.Content()
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewEducationalWalletModeScreen() {
    EducationalWalletModeScreen {}
}
