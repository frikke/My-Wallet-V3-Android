package com.blockchain.presentation.customviews.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.blockchain.analytics.Analytics
import com.blockchain.common.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.Button
import com.blockchain.componentlib.button.ButtonContent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Interest
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.Verified
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
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
    startKycClicked: () -> Unit,
    analytics: Analytics = get(scope = payloadScope),
    kycService: KycService = get(scope = payloadScope),
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
        ctaClicked = {
            highestTier?.let {
                AnalyticsType.GetVerifiedClicked.log(analytics, it)
            }
            startKycClicked()
        },
    )
}

@Composable
private fun KycUpgradeNow(
    ctaClicked: () -> Unit,
) {
    Column(
        Modifier
            .background(White)
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Row {
            Image(Icons.Filled.Verified.withTint(AppTheme.colors.primary))

            Column(
                Modifier.padding(start = AppTheme.dimensions.smallSpacing)
            ) {
                SimpleText(
                    text = stringResource(R.string.kyc_upgrade_now_sheet_title),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                )

                SimpleText(
                    modifier = Modifier.padding(top = AppTheme.dimensions.smallestSpacing),
                    text = stringResource(R.string.kyc_upgrade_now_sheet_subtitle),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
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
                    Tab.BASIC -> stringResource(R.string.kyc_upgrade_now_tab_basic)
                    Tab.VERIFIED -> stringResource(R.string.kyc_upgrade_now_tab_verified)
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
private fun Verified(ctaClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.standardSpacing)
            .background(AppTheme.colors.primary, RoundedCornerShape(AppTheme.dimensions.tinySpacing))
    ) {
        Column {
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.Transparent,
                startImageResource = Icons.Filled.Verified.withTint(White),
                primaryText = stringResource(R.string.kyc_upgrade_now_verified_level),
                primaryTextColor = White,
                endTag = TagViewState(
                    stringResource(R.string.kyc_upgrade_now_verified_full_access),
                    TagType.InfoAlt()
                ),
                secondaryTextColor = AppTheme.colors.light,
                onClick = null,
            )
            HorizontalDivider(Modifier.fillMaxWidth())
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.Transparent,
                startImageResource = Icons.Filled.Swap.withTint(White),
                primaryText = stringResource(R.string.kyc_upgrade_now_verified_swap_title),
                primaryTextColor = White,
                secondaryText = stringResource(R.string.kyc_upgrade_now_verified_swap_description),
                secondaryTextColor = AppTheme.colors.light,
                endImageResource = Icons.Check.withTint(White),
                onClick = null,
            )
            HorizontalDivider(Modifier.fillMaxWidth())
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.Transparent,
                startImageResource = Icons.Filled.Plus.withTint(White),
                primaryText = stringResource(R.string.kyc_upgrade_now_verified_buy_title),
                primaryTextColor = White,
                secondaryText = stringResource(R.string.kyc_upgrade_now_verified_buy_description),
                secondaryTextColor = AppTheme.colors.light,
                endImageResource = Icons.Check.withTint(White),
                onClick = null,
            )
            HorizontalDivider(Modifier.fillMaxWidth())
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.Transparent,
                startImageResource = Icons.Filled.Interest
                    .withTint(AppTheme.colors.primary)
                    .withBackground(
                        backgroundColor = White,
                        iconSize = AppTheme.dimensions.smallSpacing,
                        backgroundSize = AppTheme.dimensions.standardSpacing,
                    ),
                primaryText = stringResource(R.string.kyc_upgrade_now_verified_interest_title),
                primaryTextColor = White,
                secondaryText = stringResource(R.string.kyc_upgrade_now_verified_interest_description),
                secondaryTextColor = AppTheme.colors.light,
                endImageResource = Icons.Check.withTint(White),
                onClick = null,
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.standardSpacing)
                    .requiredHeightIn(min = 48.dp),
                text = stringResource(R.string.kyc_upgrade_now_verified_cta),
                onClick = ctaClicked,
                state = ButtonState.Enabled,
                defaultTextColor = AppTheme.colors.primary,
                defaultBackgroundLightColor = White,
                defaultBackgroundDarkColor = White,
                disabledTextLightAlpha = 0.7f,
                disabledTextDarkAlpha = 0.4f,
                disabledBackgroundLightColor = White,
                disabledBackgroundDarkColor = White,
                pressedBackgroundColor = White,
                buttonContent = { state: ButtonState, text: String, textColor: Color,
                    textAlpha: Float, icon: ImageResource ->
                    ButtonContent(
                        state = state,
                        text = text,
                        textColor = textColor,
                        contentAlpha = textAlpha,
                        icon = icon,
                    )
                },
            )
        }
    }
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
    Dismissed,
}

@Preview
@Composable
private fun Preview() {
    KycUpgradeNow(
        ctaClicked = {},
    )
}

@Preview
@Composable
private fun PreviewVerified() {
    Verified({})
}
