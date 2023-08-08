package com.blockchain.home.presentation.referral.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MarkdownText
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.ArrowLeft
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.CopyText
import com.blockchain.componentlib.utils.Share
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.data.DataResource
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.referral.ReferralAnalyticsEvents
import com.blockchain.home.presentation.referral.ReferralIntent
import com.blockchain.home.presentation.referral.ReferralViewModel
import com.blockchain.home.presentation.referral.ReferralViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun ReferralCode(
    viewModel: ReferralViewModel = getViewModel(scope = payloadScope),
    onBackPressed: () -> Unit
) {
    val viewState: ReferralViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ReferralIntent.LoadData())
        onDispose { }
    }

    ReferralCodeScreen(
        viewState = viewState,
        onCodeCopied = { viewModel.onIntent(ReferralIntent.CodeCopied) },
        onBackPressed = onBackPressed
    )
}

@Composable
fun ReferralCodeScreen(
    viewState: ReferralViewState,
    onCodeCopied: () -> Unit,
    onBackPressed: () -> Unit
) {
    when (viewState.referralInfo) {
        DataResource.Loading -> {
            // n/a
        }

        is DataResource.Data -> when (viewState.referralInfo.data) {
            is ReferralInfo.Data -> ReferralScreenData(
                referralInfo = viewState.referralInfo.data as ReferralInfo.Data,
                showCodeCopyConfirmation = viewState.showCodeCopyConfirmation,
                onCodeCopied = onCodeCopied,
                onBackPressed = onBackPressed
            )

            ReferralInfo.NotAvailable -> {
                // todo
            }
        }

        is DataResource.Error -> {
            // todo
        }
    }
}

@Composable
fun ReferralScreenData(
    analytics: Analytics = get(),
    referralInfo: ReferralInfo.Data,
    showCodeCopyConfirmation: Boolean,
    onCodeCopied: () -> Unit,
    onBackPressed: () -> Unit
) {
    DisposableEffect(key1 = referralInfo.campaignId) {
        analytics.logEvent(ReferralAnalyticsEvents.ReferralView(referralInfo.campaignId))
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        val backgroundUrl = referralInfo.promotionInfo?.backgroundUrl

        if (!backgroundUrl.isNullOrEmpty()) {
            AsyncMediaItem(
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
                url = backgroundUrl
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(
                    enabled = false,
                    orientation = Orientation.Vertical,
                    state = rememberScrollState()
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.padding(AppTheme.dimensions.standardSpacing)
            ) {
                Image(
                    modifier = Modifier.clickable(onClick = onBackPressed),
                    imageResource = Icons.ArrowLeft.withTint(AppColors.title)
                )
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .padding(
                        start = AppTheme.dimensions.standardSpacing,
                        end = AppTheme.dimensions.standardSpacing,
                        bottom = AppTheme.dimensions.standardSpacing
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val iconUrl = referralInfo.promotionInfo?.iconUrl
                if (!iconUrl.isNullOrEmpty()) {
                    AsyncMediaItem(
                        modifier = Modifier.size(AppTheme.dimensions.epicSpacing),
                        url = iconUrl,
                        onErrorDrawable = R.drawable.ic_referral,
                        onLoadingPlaceholder = com.blockchain.componentlib.R.drawable.ic_blockchain
                    )
                } else if (backgroundUrl.isNullOrEmpty()) {
                    Image(
                        modifier = Modifier.size(AppTheme.dimensions.epicSpacing),
                        imageResource = ImageResource.Local(R.drawable.ic_referral)
                    )
                }

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

                MarkdownText(
                    style = ComposeTypographies.Title2,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    markdownText = referralInfo.promotionInfo?.title ?: referralInfo.rewardTitle
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.verySmallSpacing))

                MarkdownText(
                    style = ComposeTypographies.Paragraph1,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    markdownText = referralInfo.promotionInfo?.message ?: referralInfo.rewardSubtitle
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    style = ComposeTypographies.Paragraph1,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    text = stringResource(com.blockchain.stringResources.R.string.referral_code_title)
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                var copyCode by remember { mutableStateOf(false) }
                if (copyCode) {
                    CopyText(
                        label = stringResource(id = com.blockchain.stringResources.R.string.referral_code_title),
                        textToCopy = referralInfo.code
                    )
                    onCodeCopied()
                    analytics.logEvent(
                        ReferralAnalyticsEvents.ReferralCopyCode(referralInfo.code, referralInfo.campaignId)
                    )
                    copyCode = false
                }
                ReferralCode(
                    code = referralInfo.code,
                    confirmCopiedToClipboard = showCodeCopyConfirmation,
                    copyToClipboard = { copyCode = true },
                    isCustomBackground = !backgroundUrl.isNullOrEmpty()
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    style = ComposeTypographies.Paragraph1,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    text = stringResource(com.blockchain.stringResources.R.string.referral_criteria_title)
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

                ReferralCriteria(
                    criteria = referralInfo.criteria,
                    isCustomBackground = !backgroundUrl.isNullOrEmpty()
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }

            var shareCode by remember { mutableStateOf(false) }
            if (shareCode) {
                Share(
                    text = stringResource(
                        com.blockchain.stringResources.R.string.referral_share_template,
                        referralInfo.code
                    ),
                    subject = stringResource(com.blockchain.stringResources.R.string.referral_share_template_subject)
                )
                analytics.logEvent(
                    ReferralAnalyticsEvents.ReferralShareCode(referralInfo.code, referralInfo.campaignId)
                )
                shareCode = false
            }
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTheme.dimensions.standardSpacing,
                        end = AppTheme.dimensions.standardSpacing,
                        top = AppTheme.dimensions.tinySpacing,
                        bottom = AppTheme.dimensions.mediumSpacing
                    ),
                text = stringResource(com.blockchain.stringResources.R.string.common_share),
                onClick = { shareCode = true }
            )
        }
    }
}

@Composable
fun ReferralCode(
    code: String,
    confirmCopiedToClipboard: Boolean,
    copyToClipboard: () -> Unit,
    isCustomBackground: Boolean
) {
    Column(
        modifier = Modifier
            .background(AppColors.backgroundSecondary)
            .padding(
                top = AppTheme.dimensions.standardSpacing,
                bottom = AppTheme.dimensions.standardSpacing
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            style = ComposeTypographies.Title4,
            color = if (isCustomBackground) ComposeColors.Light else ComposeColors.Title,
            gravity = ComposeGravities.Centre,
            text = code
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            modifier = Modifier
                .clickable { copyToClipboard() },
            style = AppTheme.typography.paragraph2,
            color = AppColors.primary,
            text = stringResource(
                if (confirmCopiedToClipboard) com.blockchain.stringResources.R.string.common_copied else
                    com.blockchain.stringResources.R.string.common_copy
            )
        )
    }
}

@Composable
fun ReferralCriteria(criteria: List<String>, isCustomBackground: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.dimensions.largeSpacing
            )
    ) {
        criteria.forEachIndexed { index, value ->
            SingleReferralCriteria(index, value, isCustomBackground)

            if (index != criteria.lastIndex) {
                ReferralCriteriaSeparator()
            }
        }
    }
}

@Composable
fun SingleReferralCriteria(
    index: Int,
    value: String,
    isCustomBackground: Boolean
) {
    Row(
        modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarkdownText(
            modifier = Modifier
                .size(AppTheme.dimensions.standardSpacing)
                .clip(CircleShape)
                .background(AppColors.backgroundSecondary),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Primary,
            markdownText = (index + 1).toString(),
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        MarkdownText(
            style = ComposeTypographies.Paragraph1,
            color = if (isCustomBackground) ComposeColors.Light else ComposeColors.Title,
            gravity = ComposeGravities.Centre,
            markdownText = value
        )
    }
}

@Composable
fun ReferralCriteriaSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 34.dp) // padding: 24 + (text width: 24 / 2) - (this view width / 2) -> 24 + 12 - 2
            .size(height = AppTheme.dimensions.tinySpacing, width = AppTheme.dimensions.smallestSpacing)
            .background(AppColors.backgroundSecondary)
    )
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(name = "Full Screen")
@Composable
fun PreviewReferralScreen() {
    ReferralScreenData(
        analytics = previewAnalytics,
        referralInfo = ReferralInfo.Data(
            rewardTitle = "Invite friends, get $30.00!",
            rewardSubtitle = "Increase your earnings on each successful invite  ",
            code = "DIEG4321",
            criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)"),
            promotionInfo = null,
            announcementInfo = null,
            campaignId = ""
        ),
        showCodeCopyConfirmation = false,
        onCodeCopied = {},
        onBackPressed = {}
    )
}

@Preview(name = "Full Screen")
@Composable
fun PreviewReferralScreenPromotion() {
    ReferralScreenData(
        analytics = previewAnalytics,
        referralInfo = ReferralInfo.Data(
            rewardTitle = "Invite friends, get $30.00!",
            rewardSubtitle = "Increase your earnings on each successful invite  ",
            code = "DIEG4321",
            criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)"),
            promotionInfo = PromotionStyleInfo(
                title = "cowboys referral title",
                message = "cowboys referral message",
                iconUrl = "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com" +
                    "/o/tickets.png?alt=media&token=b3fa42b6-55a7-4680-ba63-9d08657c0da3",
                backgroundUrl = "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com" +
                    "/o/prescott.png?alt=media&token=443cc5cb-0f04-4e46-9712-a052b2437fa1",
                actions = listOf(),
                headerUrl = "",
                foregroundColorScheme = listOf()
            ),
            announcementInfo = null,
            campaignId = ""
        ),
        showCodeCopyConfirmation = false,
        onCodeCopied = {},
        onBackPressed = {}
    )
}

@Preview(name = "Referral Code Copy")
@Composable
fun PreviewReferralCodeCopy() {
    ReferralCode(
        code = "DIEG4321",
        confirmCopiedToClipboard = false,
        copyToClipboard = {},
        isCustomBackground = false
    )
}

@Preview(name = "Referral Code Copied")
@Composable
fun PreviewReferralCodeCopied() {
    ReferralCode(
        code = "DIEG4321",
        confirmCopiedToClipboard = true,
        copyToClipboard = {},
        isCustomBackground = false
    )
}

@Preview(name = "Referral Criteria")
@Composable
fun PreviewReferralCriteria() {
    ReferralCriteria(
        criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)"),
        isCustomBackground = false
    )
}

@Preview(name = "Single Referral Criteria")
@Composable
fun PreviewSingleReferralCriteria() {
    SingleReferralCriteria(
        index = 1,
        value = "Sign up using your code",
        isCustomBackground = false
    )
}

@Preview(name = "Single Referral Criteria")
@Composable
fun PreviewSingleReferralCriteria_DarkTheme() {
    SingleReferralCriteria(
        index = 1,
        value = "Sign up using your code",
        isCustomBackground = true
    )
}
