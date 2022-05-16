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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.controls.RatingBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900
import piuk.blockchain.android.rating.presentaion.AppRatingIntents
import piuk.blockchain.android.rating.presentaion.AppRatingViewModel
import piuk.blockchain.android.rating.presentaion.R

@Composable
fun AppRatingStars(viewModel: AppRatingViewModel) {
    AppRatingStarsScreen(
        onSubmit = { rating -> viewModel.onIntent(AppRatingIntents.StarsSubmitted(rating)) },
        onCanceled = { viewModel.onIntent(AppRatingIntents.RatingCanceled) },
    )
}

/**
 * Figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=109%3A4789
 */
@Composable
fun AppRatingStarsScreen(
    onSubmit: (rating: Int) -> Unit,
    onCanceled: () -> Unit
) {
    var rating = remember { 0 }

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
        Image(
            modifier = Modifier.size(dimensionResource(R.dimen.size_medium)),
            imageResource = ImageResource.Local(R.drawable.ic_favorite_filled)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            style = AppTheme.typography.body2,
            color = Grey900,
            textAlign = TextAlign.Center,
            text = stringResource(R.string.app_rating_title)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

        Text(
            style = AppTheme.typography.paragraph1,
            color = Grey600,
            textAlign = TextAlign.Center,
            text = stringResource(R.string.app_rating_description)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

        RatingBar(
            imageFilled = R.drawable.ic_favorite_filled,
            imageOutline = R.drawable.ic_favorite_outline,
            onRatingChanged = { rating = it }
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

        Row {
            SmallMinimalButton(
                text = stringResource(R.string.common_cancel),
                onClick = onCanceled,
                state = ButtonState.Enabled
            )

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.tiny_margin)))

            SmallPrimaryButton(
                text = stringResource(R.string.common_submit),
                onClick = { onSubmit(rating) },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Full Screen", showBackground = true)
@Composable
fun PreviewAppRatingStarsScreen() {
    AppRatingStarsScreen({}, {})
}
