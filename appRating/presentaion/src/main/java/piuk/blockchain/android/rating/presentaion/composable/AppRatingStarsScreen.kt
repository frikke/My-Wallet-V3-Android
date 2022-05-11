package piuk.blockchain.android.rating.presentaion.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.RatingBar
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.button.SmallTertiaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900
import piuk.blockchain.android.rating.presentaion.R

/**
 * Figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=109%3A4789
 */
@Composable
fun AppRatingStarsScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            style = AppTheme.typography.body2,
            color = Grey900,
            text = stringResource(R.string.app_rating_title)
        )

        Text(
            style = AppTheme.typography.paragraph1,
            color = Grey600,
            text = stringResource(R.string.app_rating_description)
        )

        RatingBar(
            count = 5,
            imageFilled = R.drawable.ic_favorite_filled,
            imageOutline = R.drawable.ic_favorite_outline,
            rating = 3
        )

        Row {
            SmallTertiaryButton(
                text = stringResource(R.string.common_submit),
                onClick = { /*TODO*/ },
                state = ButtonState.Enabled
            )

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.tiny_margin)))

            SmallPrimaryButton(
                text = stringResource(R.string.common_submit),
                onClick = { /*TODO*/ },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Full Screen", showBackground = true)
@Composable
fun PreviewAppRatingStarsScreen() {
    AppRatingStarsScreen()
}
