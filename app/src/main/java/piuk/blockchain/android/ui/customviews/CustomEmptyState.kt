package piuk.blockchain.android.ui.customviews

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import piuk.blockchain.android.R

@Composable
fun CustomEmptyState(
    modifier: Modifier = Modifier.fillMaxSize(),
    @StringRes title: Int = R.string.common_empty_title,
    @StringRes description: Int = R.string.common_empty_details,
    descriptionText: String? = null,
    @DrawableRes icon: Int = R.drawable.ic_wallet_intro_image,
    @StringRes secondaryText: Int? = null,
    secondaryAction: (() -> Unit)? = null,
    @StringRes ctaText: Int = R.string.common_empty_cta,
    ctaAction: () -> Unit
) {
    Column(
        modifier
            .padding(24.dp)
            .background(Color.White)
    ) {
        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        SimpleText(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            text = descriptionText ?: stringResource(description),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
        Image(
            modifier = Modifier
                .padding(top = 8.dp)
                .weight(1f),
            imageResource = ImageResource.Local(icon)
        )
        PrimaryButton(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            text = stringResource(ctaText),
            onClick = ctaAction
        )
        if (secondaryText != null && secondaryAction != null) {
            MinimalButton(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                text = stringResource(secondaryText),
                onClick = secondaryAction
            )
        }
    }
}
