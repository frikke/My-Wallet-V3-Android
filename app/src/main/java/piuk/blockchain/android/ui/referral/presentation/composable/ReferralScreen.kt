package piuk.blockchain.android.ui.referral.presentation.composable

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MarkdownContent
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.UltraLight
import com.blockchain.domain.common.model.PromotionStyleInfo
import piuk.blockchain.android.R

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ReferralScreen(
    rewardTitle: String,
    rewardSubtitle: String,
    code: String,
    confirmCopiedToClipboard: Boolean,
    criteria: List<String>,
    onBackPressed: () -> Unit,
    copyToClipboard: (String) -> Unit,
    shareCode: (String) -> Unit,
    promotionData: PromotionStyleInfo?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val backgroundUrl = promotionData?.backgroundUrl

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
                .scrollable(enabled = false, orientation = Orientation.Vertical, state = rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {

            Box(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.standard_margin))
                    .clickable(true, onClick = onBackPressed)
            ) {
                Image(
                    ImageResource.Local(id = R.drawable.ic_arrow_back_blue)
                )
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .padding(
                        start = dimensionResource(R.dimen.standard_margin),
                        end = dimensionResource(R.dimen.standard_margin),
                        bottom = dimensionResource(R.dimen.medium_margin),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val iconUrl = promotionData?.iconUrl
                if (!iconUrl.isNullOrEmpty()) {
                    AsyncMediaItem(
                        modifier = Modifier.size(dimensionResource(R.dimen.asset_icon_size_large)),
                        url = iconUrl
                    )
                } else {
                    Image(
                        modifier = Modifier.size(dimensionResource(R.dimen.asset_icon_size_large)),
                        imageResource = ImageResource.Local(R.drawable.ic_referral)
                    )
                }

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

                MarkdownContent(
                    style = ComposeTypographies.Title2,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    markdownText = promotionData?.title ?: rewardTitle
                )

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.very_small_margin)))

                MarkdownContent(
                    style = ComposeTypographies.Paragraph1,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    markdownText = promotionData?.message ?: rewardSubtitle
                )

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

                SimpleText(
                    style = ComposeTypographies.Paragraph1,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    text = stringResource(R.string.referral_code_title),
                )

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

                ReferralCode(
                    code = code,
                    confirmCopiedToClipboard = confirmCopiedToClipboard,
                    copyToClipboard = copyToClipboard
                )

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

                SimpleText(
                    style = ComposeTypographies.Paragraph1,
                    color = if (!backgroundUrl.isNullOrEmpty()) ComposeColors.Light else ComposeColors.Title,
                    gravity = ComposeGravities.Centre,
                    text = stringResource(R.string.referral_criteria_title),
                )

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

                ReferralCriteria(
                    criteria = criteria,
                    isCustomBackground = !backgroundUrl.isNullOrEmpty()
                )

                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_margin)))
            }

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.standard_margin),
                        end = dimensionResource(id = R.dimen.standard_margin),
                        top = dimensionResource(id = R.dimen.tiny_margin),
                        bottom = dimensionResource(id = R.dimen.medium_margin)
                    ),
                text = stringResource(R.string.common_share),
                onClick = { shareCode(code) }
            )
        }
    }
}

@Composable
fun ReferralCode(
    code: String,
    confirmCopiedToClipboard: Boolean,
    copyToClipboard: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .background(UltraLight)
            .padding(
                top = dimensionResource(R.dimen.standard_margin),
                bottom = dimensionResource(R.dimen.standard_margin)
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        SimpleText(
            style = ComposeTypographies.Title4,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre,
            text = code
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            modifier = Modifier
                .clickable { copyToClipboard(code) },
            style = AppTheme.typography.paragraph2,
            color = Blue600,
            text = stringResource(if (confirmCopiedToClipboard) R.string.common_copied else R.string.common_copy),
        )
    }
}

@Composable
fun ReferralCriteria(criteria: List<String>, isCustomBackground: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.large_margin)
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
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.standard_margin)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarkdownContent(
            modifier = Modifier
                .size(AppTheme.dimensions.paddingLarge)
                .clip(CircleShape)
                .background(Blue000),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Primary,
            markdownText = (index + 1).toString(),
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

        MarkdownContent(
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
            .size(height = AppTheme.dimensions.paddingSmall, width = AppTheme.dimensions.xPaddingSmall)
            .background(Blue000)
    )
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(name = "Full Screen", showBackground = true)
@Composable
fun PreviewReferralScreen() {
    ReferralScreen(
        rewardTitle = "Invite friends, get $30.00!",
        rewardSubtitle = "Increase your earnings on each successful invite  ",
        code = "DIEG4321",
        confirmCopiedToClipboard = false,
        criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)"),
        onBackPressed = {},
        copyToClipboard = {},
        shareCode = {},
        promotionData = null
    )
}

@Preview(name = "Full Screen", showBackground = true)
@Composable
fun PreviewReferralScreenPromotion() {
    ReferralScreen(
        rewardTitle = "Invite friends, get $30.00!",
        rewardSubtitle = "Increase your earnings on each successful invite  ",
        code = "DIEG4321",
        confirmCopiedToClipboard = false,
        criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)"),
        onBackPressed = {},
        copyToClipboard = {},
        shareCode = {},
        promotionData = PromotionStyleInfo(
            "cowboys referral title",
            message = "cowboys referral message",
            iconUrl = "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com" +
                "/o/tickets.png?alt=media&token=b3fa42b6-55a7-4680-ba63-9d08657c0da3",
            headerUrl = "",
            backgroundUrl = "https://firebasestorage.googleapis.com/v0/b/fir-staging-92d79.appspot.com" +
                "/o/prescott.png?alt=media&token=443cc5cb-0f04-4e46-9712-a052b2437fa1",
            foregroundColorScheme = emptyList(),
            actions = emptyList()
        )
    )
}

@Preview(name = "Referral Code Copy", showBackground = true)
@Composable
fun PreviewReferralCodeCopy() {
    ReferralCode(
        code = "DIEG4321",
        confirmCopiedToClipboard = false,
        copyToClipboard = {}
    )
}

@Preview(name = "Referral Code Copied", showBackground = true)
@Composable
fun PreviewReferralCodeCopied() {
    ReferralCode(
        code = "DIEG4321",
        confirmCopiedToClipboard = true,
        copyToClipboard = {}
    )
}

@Preview(name = "Referral Criteria", showBackground = true)
@Composable
fun PreviewReferralCriteria() {
    ReferralCriteria(
        criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)"),
        isCustomBackground = false
    )
}

@Preview(name = "Single Referral Criteria", showBackground = true)
@Composable
fun PreviewSingleReferralCriteria() {
    SingleReferralCriteria(
        index = 1,
        value = "Sign up using your code",
        isCustomBackground = false
    )
}

@Preview(name = "Single Referral Criteria", showBackground = true)
@Composable
fun PreviewSingleReferralCriteria_DarkTheme() {
    SingleReferralCriteria(
        index = 1,
        value = "Sign up using your code",
        isCustomBackground = true
    )
}
