package com.blockchain.presentation.customviews.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Interest
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.Verified
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.getOrDefault
import com.blockchain.utils.awaitOutcome
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import timber.log.Timber

@Composable
fun KycUpgradeNowScreen(
    transactionsLimit: TransactionsLimit = TransactionsLimit.Unlimited,
    startKycClicked: () -> Unit,
    analytics: Analytics = get(scope = payloadScope),
    userIdentity: UserIdentity = get(scope = payloadScope),
    kycService: KycService = get(scope = payloadScope),
) {
    // I didn't want to create a VM just to have these 2 calls on startup, hence why they are here
    val highestTierFlow = remember {
        kycService.getHighestApprovedTierLevel(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).mapNotNull { (it as? DataResource.Data)?.data }
    }
    val highestTier by highestTierFlow.collectAsStateLifecycleAware(initial = null)

    val coroutineScope = rememberCoroutineScope()
    val isSdd = remember {
        coroutineScope.async {
            userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)
                .awaitOutcome()
                .getOrDefault(false)
        }
    }
    LaunchedEffect(Unit) {
        val isSdd = isSdd.await()
        val highestTier = highestTier ?: highestTierFlow.firstOrNull()
        if (highestTier != null) {
            AnalyticsType.Viewed.log(analytics, highestTier, isSdd)
        }
    }

    // Should never happen as there should always be cache available
    highestTier?.let { highestTier ->
        KycUpgradeNow(
            highestTier = highestTier,
            transactionsLimit = transactionsLimit,
            basicCtaClicked = {
                coroutineScope.launch {
                    AnalyticsType.GetBasicClicked.log(analytics, highestTier, isSdd.await())
                }
                startKycClicked()
            },
            verifiedCtaClicked = {
                coroutineScope.launch {
                    AnalyticsType.GetVerifiedClicked.log(analytics, highestTier, isSdd.await())
                }
                startKycClicked()
            },
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun KycUpgradeNow(
    highestTier: KycTier?,
    transactionsLimit: TransactionsLimit,
    basicCtaClicked: () -> Unit,
    verifiedCtaClicked: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
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

        val isAtleastSilver = highestTier != KycTier.BRONZE
        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            pagerState.scrollToPage(Tab.VERIFIED.ordinal)
        }
        var isInitialisingPager by remember { mutableStateOf(true) }

        CustomTabLayout(pagerState, coroutineScope)

        HorizontalPager(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = AppTheme.dimensions.tinySpacing),
            state = pagerState,
            count = Tab.values().size,
            itemSpacing = AppTheme.dimensions.smallSpacing,
            verticalAlignment = Alignment.Top,
        ) { page ->
            // We've gotta do it like this because there's no way of setting the initial page on the Pager and changing
            // pages is not synchronous, and we were seeing a quick flash between Basic and Verified when first rendered
            if (isInitialisingPager) {
                Verified(verifiedCtaClicked)
                isInitialisingPager = false
                return@HorizontalPager
            }
            when (Tab.values()[page]) {
                Tab.BASIC -> Basic(isAtleastSilver, transactionsLimit, basicCtaClicked)
                Tab.VERIFIED -> Verified(verifiedCtaClicked)
            }
        }
    }
}

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

@Composable
private fun Basic(
    isBasicApproved: Boolean,
    transactionsLimit: TransactionsLimit,
    ctaClicked: () -> Unit,
) {
    Column {
        Column(
            Modifier
                .background(White, RoundedCornerShape(AppTheme.dimensions.tinySpacing))
                .border(1.dp, AppTheme.colors.light, RoundedCornerShape(AppTheme.dimensions.tinySpacing))
        ) {
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Verified.withTint(Grey400),
                primaryText = stringResource(R.string.kyc_upgrade_now_basic_level),
                endTag = TagViewState(
                    stringResource(
                        if (isBasicApproved) R.string.kyc_upgrade_now_basic_active
                        else R.string.kyc_upgrade_now_basic_limited_access
                    ),
                    TagType.InfoAlt()
                ),
                onClick = null,
            )
            HorizontalDivider(Modifier.fillMaxWidth())
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Send.withTint(AppTheme.colors.primary),
                primaryText = stringResource(R.string.kyc_upgrade_now_basic_send_receive_title),
                secondaryText = stringResource(R.string.kyc_upgrade_now_basic_send_receive_description),
                endImageResource = Icons.Check.withTint(AppTheme.colors.primary),
                onClick = null,
            )
            HorizontalDivider(Modifier.fillMaxWidth())
            DefaultTableRow(
                modifier = Modifier.fillMaxWidth(),
                startImageResource = Icons.Filled.Swap.withTint(AppTheme.colors.primary),
                primaryText = stringResource(R.string.kyc_upgrade_now_basic_swap_title),
                secondaryText = stringResource(R.string.kyc_upgrade_now_basic_swap_description),
                endImageResource = Icons.Check.withTint(AppTheme.colors.primary),
                onClick = null,
            )

            if (!isBasicApproved) {
                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.standardSpacing),
                    text = stringResource(R.string.kyc_upgrade_now_basic_cta),
                    onClick = ctaClicked
                )
            }
        }

        if (transactionsLimit is TransactionsLimit.Limited) {
            TransactionsLeft(
                maxTransactions = transactionsLimit.maxTransactionsCap,
                transactionsLeft = transactionsLimit.maxTransactionsLeft
            )
        }
    }
}

@Composable
private fun TransactionsLeft(
    maxTransactions: Int,
    transactionsLeft: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.tinySpacing)
            .background(White, RoundedCornerShape(AppTheme.dimensions.tinySpacing))
            .border(1.dp, AppTheme.colors.light, RoundedCornerShape(AppTheme.dimensions.tinySpacing))
            .padding(
                vertical = AppTheme.dimensions.smallSpacing,
                horizontal = AppTheme.dimensions.standardSpacing,
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val progress = transactionsLeft.toFloat() / maxTransactions.toFloat()
            val backgroundColor = AppTheme.colors.medium
            val progressColor = AppTheme.colors.primary

            Box(modifier = Modifier.size(AppTheme.dimensions.xHugeSpacing)) {
                CircularProgressIndicator(
                    modifier = Modifier.matchParentSize(),
                    color = backgroundColor,
                    strokeWidth = 6.dp,
                    progress = 1f,
                )
                CircularProgressIndicator(
                    modifier = Modifier.matchParentSize(),
                    color = progressColor,
                    strokeWidth = 6.dp,
                    progress = progress,
                )

                SimpleText(
                    modifier = Modifier.align(Alignment.Center),
                    text = "$transactionsLeft",
                    style = ComposeTypographies.Body2,
                    color = ComposeColors.Primary,
                    gravity = ComposeGravities.Centre,
                )
            }

            Column(
                Modifier.weight(1f)
            ) {
                SimpleText(
                    modifier = Modifier.padding(
                        start = AppTheme.dimensions.smallSpacing,
                        bottom = AppTheme.dimensions.composeSmallestSpacing,
                    ),
                    text = stringResource(R.string.transactions_left_title),
                    style = ComposeTypographies.Body2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                )
                SimpleText(
                    modifier = Modifier.padding(
                        start = AppTheme.dimensions.smallSpacing,
                        top = AppTheme.dimensions.composeSmallestSpacing,
                    ),
                    text = stringResource(R.string.transactions_left_subtitle),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                )
            }
        }
    }
}

@Composable
private fun Verified(ctaClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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

private fun AnalyticsType.log(analytics: Analytics, highestTier: KycTier, isSdd: Boolean) {
    val event = when (this) {
        AnalyticsType.GetBasicClicked -> KycUpgradeNowGetBasicClicked(highestTier, isSdd)
        AnalyticsType.GetVerifiedClicked -> KycUpgradeNowGetVerifiedClicked(highestTier, isSdd)
        AnalyticsType.Viewed -> KycUpgradeNowViewed(highestTier, isSdd)
        AnalyticsType.Dismissed -> KycUpgradeNowDismissed(highestTier, isSdd)
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
        highestTier = KycTier.SILVER,
        transactionsLimit = TransactionsLimit.Limited(3, 2),
        basicCtaClicked = {},
        verifiedCtaClicked = {},
    )
}

@Preview
@Composable
private fun PreviewBasicT0() {
    Basic(false, TransactionsLimit.Unlimited, {})
}

@Preview
@Composable
private fun PreviewBasicT1() {
    Basic(true, TransactionsLimit.Limited(3, 1), {})
}

@Preview
@Composable
private fun PreviewVerified() {
    Verified({})
}

private enum class Tab {
    BASIC,
    VERIFIED,
}
