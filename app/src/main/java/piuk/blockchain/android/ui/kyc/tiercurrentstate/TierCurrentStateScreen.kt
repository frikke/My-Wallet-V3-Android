package piuk.blockchain.android.ui.kyc.tiercurrentstate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.AnnotatedStringUtils
import com.blockchain.nabu.models.responses.nabu.KycState
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.commonui.UserIcon
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_GOLD_UNAVAILABLE_SUPPORT
import piuk.blockchain.android.util.StringUtils

@Composable
fun TierCurrentStateScreen(
    state: KycState,
    isSddVerified: Boolean,
    underReviewCtaClicked: () -> Unit,
    verifiedCtaClicked: () -> Unit,
    rejectedCtaClicked: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            KycState.None,
            KycState.Pending,
            KycState.UnderReview -> UnderReviewScreen(isSddVerified, underReviewCtaClicked)
            KycState.Verified -> VerifiedScreen(verifiedCtaClicked)
            KycState.Expired,
            KycState.Rejected -> RejectedScreen(rejectedCtaClicked)
        }
    }
}

@Composable
private fun ColumnScope.UnderReviewScreen(
    isSddVerified: Boolean,
    underReviewCtaClicked: () -> Unit
) {
    Header(state = KycState.UnderReview)

    Spacer(Modifier.weight(1f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = AppTheme.dimensions.xHugeSpacing,
                start = AppTheme.dimensions.standardSpacing,
                end = AppTheme.dimensions.standardSpacing
            )
            .background(
                color = AppTheme.colors.medium,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(1.dp)
            .background(
                color = AppTheme.colors.light,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(all = AppTheme.dimensions.smallSpacing)
    ) {
        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.kyc_tier_current_state_underreview_nextstep_title),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.tinySpacing),
            text = stringResource(
                if (isSddVerified) R.string.kyc_tier_current_state_underreview_nextstep_subtitle_sdd
                else R.string.kyc_tier_current_state_underreview_nextstep_subtitle
            ),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
    }

    PrimaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = AppTheme.dimensions.standardSpacing,
                start = AppTheme.dimensions.standardSpacing,
                end = AppTheme.dimensions.standardSpacing,
            ),
        text = stringResource(R.string.kyc_tier_current_state_underreview_cta),
        onClick = underReviewCtaClicked
    )
}

@Composable
private fun ColumnScope.VerifiedScreen(verifiedCtaClicked: () -> Unit) {
    Header(state = KycState.Verified)

    Spacer(Modifier.weight(1f))

    PrimaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = AppTheme.dimensions.standardSpacing,
                start = AppTheme.dimensions.standardSpacing,
                end = AppTheme.dimensions.standardSpacing,
            ),
        text = stringResource(R.string.kyc_tier_current_state_verified_cta),
        onClick = verifiedCtaClicked
    )
}

@Composable
private fun ColumnScope.RejectedScreen(rejectedCtaClicked: () -> Unit) {
    Header(state = KycState.Rejected)

    Spacer(Modifier.weight(1f))

    PrimaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = AppTheme.dimensions.standardSpacing,
                start = AppTheme.dimensions.standardSpacing,
                end = AppTheme.dimensions.standardSpacing,
            ),
        text = stringResource(R.string.kyc_tier_current_state_rejected_cta),
        onClick = rejectedCtaClicked
    )
}

@Composable
private fun ColumnScope.Header(
    modifier: Modifier = Modifier,
    state: KycState
) {
    val context = LocalContext.current
    val titleRes = when (state) {
        KycState.None,
        KycState.Pending,
        KycState.UnderReview -> R.string.kyc_tier_current_state_underreview_title
        KycState.Verified -> R.string.kyc_tier_current_state_verified_title
        KycState.Expired,
        KycState.Rejected -> R.string.kyc_tier_current_state_rejected_title
    }
    val subtitle = when (state) {
        KycState.None,
        KycState.Pending,
        KycState.UnderReview -> AnnotatedString(stringResource(R.string.kyc_tier_current_state_underreview_subtitle))
        KycState.Verified -> AnnotatedString(stringResource(R.string.kyc_tier_current_state_verified_subtitle))
        KycState.Expired,
        KycState.Rejected -> AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
            LocalContext.current,
            R.string.kyc_tier_current_state_rejected_subtitle,
            mapOf("gold_error" to URL_BLOCKCHAIN_GOLD_UNAVAILABLE_SUPPORT)
        )
    }

    val userStatusIconRes = when (state) {
        KycState.None,
        KycState.Pending,
        KycState.UnderReview -> R.drawable.ic_pending_clock
        KycState.Expired,
        KycState.Rejected -> R.drawable.ic_warning_info_circle
        KycState.Verified -> R.drawable.ic_check_circle
    }
    UserIcon(
        modifier = Modifier.padding(top = AppTheme.dimensions.xHugeSpacing),
        iconRes = R.drawable.ic_bank_user,
        statusIconRes = userStatusIconRes,
    )

    SimpleText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.largeSpacing)
            .padding(horizontal = AppTheme.dimensions.xHugeSpacing),
        text = stringResource(titleRes),
        style = ComposeTypographies.Title3,
        color = ComposeColors.Title,
        gravity = ComposeGravities.Centre
    )

    SimpleText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.tinySpacing)
            .padding(horizontal = AppTheme.dimensions.xHugeSpacing),
        text = subtitle,
        style = ComposeTypographies.Body1,
        color = ComposeColors.Body,
        gravity = ComposeGravities.Centre,
        onAnnotationClicked = { tag, value ->
            if (tag == StringUtils.TAG_URL) {
                Intent(Intent.ACTION_VIEW, Uri.parse(value))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    .also { context.startActivity(it) }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewUnderReview() {
    TierCurrentStateScreen(
        state = KycState.UnderReview,
        isSddVerified = true,
        underReviewCtaClicked = {},
        verifiedCtaClicked = {},
        rejectedCtaClicked = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewRejected() {
    TierCurrentStateScreen(
        state = KycState.Rejected,
        isSddVerified = true,
        underReviewCtaClicked = {},
        verifiedCtaClicked = {},
        rejectedCtaClicked = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewVerified() {
    TierCurrentStateScreen(
        state = KycState.Verified,
        isSddVerified = true,
        underReviewCtaClicked = {},
        verifiedCtaClicked = {},
        rejectedCtaClicked = {},
    )
}
