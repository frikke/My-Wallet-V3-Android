package piuk.blockchain.android.rating.presentaion.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900
import piuk.blockchain.android.rating.presentaion.AppRatingIntents
import piuk.blockchain.android.rating.presentaion.AppRatingViewModel
import piuk.blockchain.android.rating.presentaion.R

private const val MAX_FEEDBACK_LENGTH = 1800

@Composable
fun AppRatingFeedback(viewModel: AppRatingViewModel) {
    AppRatingFeedbackScreen { feedback ->
        viewModel.onIntent(AppRatingIntents.FeedbackSubmitted(feedback))
    }
}

/**
 * Figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=109%3A5688
 */
@Composable
fun AppRatingFeedbackScreen(
    onSubmit: (feedback: String) -> Unit
) {
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.standard_spacing),
                top = dimensionResource(R.dimen.standard_spacing),
                end = dimensionResource(R.dimen.standard_spacing),
                bottom = dimensionResource(R.dimen.small_spacing)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            style = AppTheme.typography.body2,
            color = Grey900,
            textAlign = TextAlign.Center,
            text = stringResource(R.string.app_rating_feedback_title)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_spacing)))

        Text(
            style = AppTheme.typography.paragraph1,
            color = Grey600,
            textAlign = TextAlign.Center,
            text = stringResource(R.string.app_rating_feedback_description)
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_spacing)))

        TextInput(
            modifier = Modifier.height(160.dp),
            value = feedback,
            onValueChange = { feedback = it },
            label = stringResource(R.string.app_rating_feedback_hint),
            maxLength = MAX_FEEDBACK_LENGTH
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_spacing)))

        SmallPrimaryButton(
            text = stringResource(R.string.common_submit),
            onClick = { onSubmit(feedback) },
            state = ButtonState.Enabled
        )
    }
}

@Preview(name = "Feedback Screen", showBackground = true)
@Composable
fun PreviewAppRatingFeedbackScreen() {
    AppRatingFeedbackScreen {}
}
