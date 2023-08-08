package com.blockchain.earn.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.SingleIconTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.componentlib.utils.value
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class)
@Composable
fun EarnProductComparator(
    products: List<EarnProductUiElement>,
    onLearnMore: () -> Unit,
    onClose: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background, shape = AppTheme.shapes.large.topOnly()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            title = stringResource(com.blockchain.stringResources.R.string.earn_product_comparator_title),
            onClosePress = onClose,
        )

        LargeVerticalSpacer()

        val pagerState = rememberPagerState()
        HorizontalPager(
            count = products.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F, true)
                .padding(horizontal = AppTheme.dimensions.verySmallSpacing),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            EarnProductComparatorPage(
                product = products[pageIndex],
                modifier = Modifier
                    .padding(AppTheme.dimensions.verySmallSpacing)
                    .graphicsLayer {
                        val pageOffset = calculateCurrentOffsetForPage(pageIndex).absoluteValue

                        // animate the scaleX + scaleY, between 70% and 100%
                        lerp(
                            start = 0.70f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        ).also { scale ->
                            scaleX = scale
                            scaleY = scale
                        }

                        // animate the alpha, between 20% and 100%
                        alpha = lerp(
                            start = 0.2f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
        ) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                inactiveColor = AppTheme.colors.medium,
                activeColor = AppTheme.colors.primary,
                indicatorWidth = AppTheme.dimensions.verySmallSpacing
            )

            SmallVerticalSpacer()

            MinimalPrimaryButton(
                text = stringResource(com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMore,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EarnProductComparatorPage(product: EarnProductUiElement, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = AppColors.backgroundSecondary,
        shape = AppTheme.shapes.large,
        border = BorderStroke(1.dp, AppColors.medium),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                color = AppColors.background,
                shape = AppTheme.shapes.large,
                border = BorderStroke(1.dp, AppColors.medium),
            ) {
                SingleIconTableRow(
                    primaryText = product.header.primaryText.value(),
                    secondaryText = product.header.secondaryText?.value(),
                    imageResource = product.header.imageResource,
                    tint = AppColors.primary,
                    backgroundColor = Color.Transparent
                )
            }

            SingleIconTableRow(
                primaryText = product.targetAudience.primaryText.value(),
                imageResource = product.targetAudience.imageResource
            )

            SingleIconTableRow(
                primaryText = product.availableAssets.primaryText.value(),
                imageResource = product.availableAssets.imageResource
            )

            SingleIconTableRow(
                primaryText = product.earnRate.primaryText.value(),
                secondaryText = product.earnRate.secondaryText?.value(),
                imageResource = product.earnRate.imageResource
            )

            SingleIconTableRow(
                primaryText = product.earnFrequency.primaryText.value(),
                imageResource = product.earnFrequency.imageResource
            )

            SingleIconTableRow(
                primaryText = product.payoutFrequency.primaryText.value(),
                imageResource = product.payoutFrequency.imageResource
            )

            SingleIconTableRow(
                primaryText = product.withdrawalFrequency.primaryText.value(),
                imageResource = product.withdrawalFrequency.imageResource
            )

            SmallVerticalSpacer()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EarnProductComparatorPreview() {
    EarnProductComparator(
        products = listOf(
            EarnProductUiElement.PassiveRewardsUiElement(7.1),
            EarnProductUiElement.StakingRewardsUiElement(7.1),
            EarnProductUiElement.ActiveRewardsUiElement(7.1)
        ),
        onLearnMore = {},
        onClose = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnProductComparatorPreviewDark() {
    EarnProductComparatorPreview()
}

@Preview(showBackground = true)
@Composable
fun EarnProductComparatorPagePreview() {
    AppTheme {
        EarnProductComparatorPage(EarnProductUiElement.PassiveRewardsUiElement(7.1))
    }
}
