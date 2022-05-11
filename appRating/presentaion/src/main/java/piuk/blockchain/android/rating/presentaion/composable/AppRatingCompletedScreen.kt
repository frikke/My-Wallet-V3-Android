package piuk.blockchain.android.rating.presentaion.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.RatingBar
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900
import piuk.blockchain.android.rating.presentaion.R

/**
 * Figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=133%3A5341
 */
@Composable
fun AppRatingCompletedScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.standard_margin),
                top = dimensionResource(R.dimen.standard_margin),
                end = dimensionResource(R.dimen.standard_margin),
                bottom = dimensionResource(R.dimen.small_margin)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            style = AppTheme.typography.body2,
            color = Grey900,
            text = stringResource(R.string.app_rating_completed_title)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

        Text(
            style = AppTheme.typography.paragraph1,
            color = Grey600,
            text = stringResource(R.string.app_rating_completed_description)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

        SmallPrimaryButton(
            text = stringResource(R.string.common_submit),
            onClick = { /*TODO*/ },
            state = ButtonState.Enabled
        )
    }
}


@Preview(name = "Full Screen", showBackground = true)
@Composable
fun PreviewAppRatingCompletedScreen() {
    AppRatingCompletedScreen()
}
