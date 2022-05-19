package piuk.blockchain.android.rating.presentaion.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900
import piuk.blockchain.android.rating.presentaion.AppRatingIntents
import piuk.blockchain.android.rating.presentaion.AppRatingViewModel
import piuk.blockchain.android.rating.presentaion.R

@Composable
fun AppRatingCompleted(
    viewModel: AppRatingViewModel,
    withFeedback: Boolean
) {
    val viewState = viewModel.viewState.collectAsState().value

    AppRatingCompletedScreen(
        withFeedback = withFeedback,
        isLoading = viewState.isLoading,
        onSubmit = { viewModel.onIntent(AppRatingIntents.RatingCompleted) }
    )
}

/**
 * Figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=133%3A5341
 */
@Composable
fun AppRatingCompletedScreen(
    withFeedback: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
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
            text = stringResource(
                if (withFeedback) R.string.app_rating_completed_feedback_title
                else R.string.app_rating_completed_no_feedback_title
            )
        )

        if (withFeedback) {
            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

            Text(
                style = AppTheme.typography.paragraph1,
                color = Grey600,
                textAlign = TextAlign.Center,
                text = stringResource(R.string.app_rating_completed_feedback_description)
            )
        }

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

        SmallPrimaryButton(
            text = stringResource(R.string.done),
            onClick = onSubmit,
            state = if (isLoading) ButtonState.Loading else ButtonState.Enabled
        )
    }
}

@Preview(name = "Completed Screen With Feedback", showBackground = true)
@Composable
fun PreviewAppRatingCompletedScreenWithFeedback() {
    AppRatingCompletedScreen(withFeedback = true, isLoading = false) {}
}

@Preview(name = "Completed Screen With Feedback Loading", showBackground = true)
@Composable
fun PreviewAppRatingCompletedScreenWithFeedbackLoading() {
    AppRatingCompletedScreen(withFeedback = true, isLoading = true) {}
}

@Preview(name = "Completed Screen Without Feedback", showBackground = true)
@Composable
fun PreviewAppRatingCompletedScreenWithoutFeedback() {
    AppRatingCompletedScreen(withFeedback = false, isLoading = false) {}
}

@Preview(name = "Completed Screen Without Feedback Loading", showBackground = true)
@Composable
fun PreviewAppRatingCompletedScreenWithoutFeedbackLoading() {
    AppRatingCompletedScreen(withFeedback = false, isLoading = true) {}
}
