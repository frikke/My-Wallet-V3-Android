package piuk.blockchain.android.ui.referral.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.UltraLight
import com.blockchain.utils.isNotLastIn
import kotlin.math.max
import piuk.blockchain.android.R

/**
 * Figma: https://www.figma.com/file/myZmBbJrKunDZfUPyARL6Y/AND-%7C-Referrals?node-id=3%3A14841
 */
@Composable
fun ReferralScreen(
    rewardTitle: String,
    rewardSubtitle: String,
    code: String,
    confirmCopiedToClipboard: Boolean,
    criteria: List<String>,
    onBackPressed: () -> Unit,
    copyToClipboard: (String) -> Unit,
    shareCode: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(enabled = false, orientation = Orientation.Vertical, state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavigationBar(title = stringResource(id = R.string.empty), onBackButtonClick = onBackPressed)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .weight(weight = 1f, fill = false)
                .padding(
                    top = dimensionResource(R.dimen.zero_margin),
                    start = dimensionResource(R.dimen.standard_margin),
                    end = dimensionResource(R.dimen.standard_margin),
                    bottom = dimensionResource(R.dimen.medium_margin),
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.size(dimensionResource(R.dimen.asset_icon_size_large)),
                imageResource = ImageResource.Local(
                    R.drawable.ic_referral
                )
            )

            Spacer(modifier = Modifier.size(55.dp))

            Text(
                style = AppTheme.typography.title2,
                color = Grey900,
                text = rewardTitle,
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.very_small_margin)))

            Text(
                style = AppTheme.typography.paragraph1,
                color = Grey900,
                text = rewardSubtitle,
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.huge_margin)))

            Text(
                style = AppTheme.typography.paragraph1,
                color = Grey600,
                text = stringResource(R.string.referral_code_title),
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

            ReferralCode(
                code = code,
                confirmCopiedToClipboard = confirmCopiedToClipboard,
                copyToClipboard = copyToClipboard
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.huge_margin)))

            Text(
                style = AppTheme.typography.paragraph1,
                color = Grey600,
                text = stringResource(R.string.referral_criteria_title),
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

            ReferralCriteria(criteria)

            Spacer(modifier = Modifier.size(111.dp))
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

        Text(
            style = AppTheme.typography.title4,
            color = Grey900,
            text = code,
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
fun ReferralCriteria(criteria: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.large_margin)
            )
    ) {
        criteria.forEachIndexed { index, value ->
            SingleReferralCriteria(index, value)

            if (index isNotLastIn criteria) {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))
            }
        }
    }
}

@Composable
fun SingleReferralCriteria(
    index: Int,
    value: String
) {
    Row(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.standard_margin)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .background(Blue000)
                .layout { measurable, constraints ->
                    with(measurable.measure(constraints)) {
                        val size = max(width, height)

                        layout(width = size, height = size) {
                            placeRelative(
                                x = size / 2 - width / 2,
                                y = size / 2 - height / 2
                            )
                        }
                    }
                },
            style = AppTheme.typography.body2,
            color = Blue600,
            text = (index + 1).toString(),
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

        Text(
            style = AppTheme.typography.paragraph1,
            color = Grey900,
            text = value,
        )
    }
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
        shareCode = {}
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
        criteria = listOf("Sign up using your code", "Verify their identity", "Trade (min 50)")
    )
}

@Preview(name = "Single Referral Criteria", showBackground = true)
@Composable
fun PreviewSingleReferralCriteria() {
    SingleReferralCriteria(
        index = 1,
        value = "Sign up using your code"
    )
}
