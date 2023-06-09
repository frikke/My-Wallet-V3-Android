package com.blockchain.earn.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.SingleIconTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.HugeVerticalSpacer
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.UltraLight
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
            .background(AppTheme.colors.background, shape = AppTheme.shapes.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            title = stringResource(com.blockchain.stringResources.R.string.earn_product_comparator_title),
            onClosePress = onClose,
            shouldShowDivider = false
        )

        LargeVerticalSpacer()

        val pagerState = rememberPagerState()
        HorizontalPager(
            count = products.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
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

        HugeVerticalSpacer()

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

@Preview(showBackground = true)
@Composable
fun EarnProductComparatorPreview() {
    AppTheme {
        EarnProductComparator(
            products = listOf(
                EarnProductUiElement.PassiveRewardsUiElement,
                EarnProductUiElement.StakingRewardsUiElement,
                EarnProductUiElement.ActiveRewardsUiElement
            ),
            onLearnMore = {},
            onClose = {}
        )
    }
}

@Composable
fun EarnProductComparatorPage(product: EarnProductUiElement, modifier: Modifier = Modifier) {
    Card(
        elevation = 0.dp,
        border = BorderStroke(1.dp, Grey300),
        shape = AppTheme.shapes.large,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Card(
                elevation = 0.dp,
                border = BorderStroke(1.dp, Grey300),
                shape = AppTheme.shapes.large
            ) {
                SingleIconTableRow(
                    primaryText = stringResource(id = product.header.primaryTextId),
                    secondaryText = product.header.secondaryTextId?.let { stringResource(id = it) },
                    imageResource = product.header.imageResource,
                    backgroundColor = UltraLight
                )
            }

            SingleIconTableRow(
                primaryText = stringResource(id = product.targetAudience.primaryTextId),
                imageResource = product.targetAudience.imageResource
            )

            SingleIconTableRow(
                primaryText = stringResource(id = product.availableAssets.primaryTextId),
                imageResource = product.availableAssets.imageResource
            )

            SingleIconTableRow(
                primaryText = stringResource(id = product.earnRate.primaryTextId),
                secondaryText = product.earnRate.secondaryTextId?.let { stringResource(id = it) },
                imageResource = product.earnRate.imageResource
            )

            SingleIconTableRow(
                primaryText = stringResource(id = product.earnFrequency.primaryTextId),
                imageResource = product.earnFrequency.imageResource
            )

            SingleIconTableRow(
                primaryText = stringResource(id = product.payoutFrequency.primaryTextId),
                imageResource = product.payoutFrequency.imageResource
            )

            SingleIconTableRow(
                primaryText = stringResource(id = product.withdrawalFrequency.primaryTextId),
                imageResource = product.withdrawalFrequency.imageResource
            )

            SmallVerticalSpacer()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EarnProductComparatorPagePreview() {
    AppTheme {
        EarnProductComparatorPage(EarnProductUiElement.PassiveRewardsUiElement)
    }
}

@Composable
fun EarnProductComparatorCta() {
}
