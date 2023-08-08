package piuk.blockchain.android.ui.referral.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun ReferralInviteNowScreen(
    referralTitle: String,
    referralSubtitle: String,
    onPositiveAction: () -> Unit,
    onNegativeAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.primary,
                shape = RoundedCornerShape(dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing))
            )
    ) {
        SheetHeader(
            onClosePress = onNegativeAction
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.xlarge_spacing)))

            Image(
                imageResource = ImageResource.Local(
                    R.drawable.ic_referral_inverted
                )
            )

            Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.xlarge_spacing)))

            Text(
                style = AppTheme.typography.title2,
                color = Color.White,
                text = referralTitle
            )

            Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)))

            Text(
                style = AppTheme.typography.paragraph1,
                color = Color.White,
                text = referralSubtitle
            )

            Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.large_spacing)))

            MinimalPrimaryButton(
                text = stringResource(com.blockchain.stringResources.R.string.referral_invite_now),
                onClick = onPositiveAction,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing)))

            PrimaryButton(
                text = stringResource(com.blockchain.stringResources.R.string.common_skip),
                onClick = onNegativeAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun PreviewReferralPopupScreen() {
    ReferralInviteNowScreen(
        referralTitle = "Invite friends, get \$30!",
        referralSubtitle = "Increase your earnings on each successful invite",
        onPositiveAction = { },
        onNegativeAction = { }
    )
}
