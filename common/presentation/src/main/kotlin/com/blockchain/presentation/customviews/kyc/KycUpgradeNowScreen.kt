package com.blockchain.presentation.customviews.kyc

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Interest
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.Verified
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.sheets.SheetNub
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.koin.payloadScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import org.koin.androidx.compose.get

@Composable
fun KycUpgradeNowScreen(
    isBottomSheet: Boolean = false,
    startKycClicked: () -> Unit,
    analytics: Analytics = get(scope = payloadScope),
    kycService: KycService = get(scope = payloadScope)
) {
    // I didn't want to create a VM just to have this call on startup, hence why it's here
    val highestTierFlow = remember {
        kycService.getHighestApprovedTierLevel(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).mapNotNull { (it as? DataResource.Data)?.data }
    }
    val highestTier by highestTierFlow.collectAsStateLifecycleAware(initial = null)

    LaunchedEffect(Unit) {
        val highestTier = highestTier ?: highestTierFlow.firstOrNull()
        if (highestTier != null) {
            AnalyticsType.Viewed.log(analytics, highestTier)
        }
    }

    KycUpgradeNow(
        isBottomSheet = isBottomSheet,
        ctaClicked = {
            highestTier?.let {
                AnalyticsType.GetVerifiedClicked.log(analytics, it)
            }
            startKycClicked()
        }
    )
}

@Composable
private fun KycUpgradeNow(
    isBottomSheet: Boolean = false,
    ctaClicked: () -> Unit
) {
    Column(
        Modifier
            .background(AppColors.background)
            .padding(AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isBottomSheet) {
            SheetNub()
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        }
        Row {
            Image(Icons.Filled.Verified.withTint(AppTheme.colors.primary))

            Column(
                Modifier.padding(start = AppTheme.dimensions.smallSpacing)
            ) {
                SimpleText(
                    text = stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_sheet_title),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )

                SimpleText(
                    modifier = Modifier.padding(top = AppTheme.dimensions.smallestSpacing),
                    text = stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_sheet_subtitle),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
            }
        }

        Verified(ctaClicked)
    }
}

/* (aromano): Disabling since it's not used anymore, but leaving it in
              because this custom tablayout might be used in the future
@OptIn(ExperimentalPagerApi::class)
@Composable
fun CustomTabLayout(pagerState: PagerState, coroutineScope: CoroutineScope) {
    var tabLayoutWidthPx by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.xHugeSpacing)
            .height(AppTheme.dimensions.hugeSpacing)
            .background(AppTheme.colors.medium, RoundedCornerShape(AppTheme.dimensions.largeSpacing))
            .padding(all = AppTheme.dimensions.composeSmallestSpacing)
            .onSizeChanged { tabLayoutWidthPx = it.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        val indicatorWidth = with(LocalDensity.current) {
            val usableWidth = tabLayoutWidthPx.toDp()
            usableWidth / Tab.values().size
        }
        Box(
            modifier = Modifier
                .width(width = indicatorWidth)
                .fillMaxHeight()
                .offset(x = (pagerState.currentPage * indicatorWidth) + (pagerState.currentPageOffset * indicatorWidth))
                .background(White, RoundedCornerShape(AppTheme.dimensions.largeSpacing))
        )

        Row(Modifier.fillMaxWidth()) {
            Tab.values().forEach { tab ->
                val text = when (tab) {
                    Tab.BASIC -> stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_tab_basic)
                    Tab.VERIFIED -> stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_tab_verified)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                    contentAlignment = Alignment.Center,
                ) {
                    SimpleText(
                        text = text,
                        style = ComposeTypographies.Paragraph2,
                        color = if (pagerState.currentPage == tab.ordinal) {
                            ComposeColors.Primary
                        } else {
                            ComposeColors.Body
                        },
                        gravity = ComposeGravities.Centre,
                    )
                }
            }
        }
    }
}
*/

@Composable
private fun ColumnScope.Verified(ctaClicked: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.standardSpacing),
        shape = AppTheme.shapes.medium,
        color = Color.Transparent
    ) {
        Column {
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Verified.withTint(AppColors.primary),
                primaryText = stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_level),
                endTag = TagViewState(
                    stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_full_access),
                    TagType.InfoAlt()
                ),
                onClick = null
            )
            AppDivider()
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Swap.withTint(AppColors.primary),
                primaryText = stringResource(
                    com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_swap_title
                ),
                secondaryText = stringResource(
                    com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_swap_description
                ),
                endImageResource = Icons.Check.withTint(AppColors.body),
                onClick = null
            )
            AppDivider()
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Plus.withTint(AppColors.primary),
                primaryText = stringResource(
                    com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_buy_title
                ),
                secondaryText = stringResource(
                    com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_buy_description
                ),
                endImageResource = Icons.Check.withTint(AppColors.body),
                onClick = null
            )
            AppDivider()
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Interest
                    .withTint(AppColors.backgroundSecondary)
                    .withBackground(
                        backgroundColor = AppColors.primary,
                        iconSize = AppTheme.dimensions.smallSpacing,
                        backgroundSize = AppTheme.dimensions.standardSpacing
                    ),
                primaryText = stringResource(
                    com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_interest_title
                ),
                secondaryText = stringResource(
                    com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_interest_description
                ),
                endImageResource = Icons.Check.withTint(AppColors.body),
                onClick = null
            )
        }
    }

    PrimaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.standardSpacing),
        text = stringResource(com.blockchain.stringResources.R.string.kyc_upgrade_now_verified_cta),
        onClick = ctaClicked
    )
}

private fun AnalyticsType.log(analytics: Analytics, highestTier: KycTier) {
    val event = when (this) {
        AnalyticsType.GetBasicClicked -> KycUpgradeNowGetBasicClicked(highestTier)
        AnalyticsType.GetVerifiedClicked -> KycUpgradeNowGetVerifiedClicked(highestTier)
        AnalyticsType.Viewed -> KycUpgradeNowViewed(highestTier)
        AnalyticsType.Dismissed -> KycUpgradeNowDismissed(highestTier)
    }
    analytics.logEvent(event)
}

private enum class AnalyticsType {
    GetBasicClicked,
    GetVerifiedClicked,
    Viewed,
    Dismissed
}

@Preview
@Composable
private fun PreviewKycUpgradeNow() {
    KycUpgradeNow(
        ctaClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewKycUpgradeNowDark() {
    PreviewKycUpgradeNow()
}
