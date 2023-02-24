package piuk.blockchain.android.ui.customviews

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun CustomEmptyState(
    modifier: Modifier = Modifier.fillMaxSize(),
    @StringRes title: Int = R.string.common_empty_title,
    @StringRes description: Int = R.string.common_empty_details,
    descriptionText: String? = null,
    @DrawableRes icon: Int = Icons.Filled.User.id,
    @StringRes secondaryText: Int? = null,
    secondaryAction: (() -> Unit)? = null,
    @StringRes ctaText: Int = R.string.common_empty_cta,
    ctaAction: () -> Unit
) {
    Column(
        modifier
            .padding(AppTheme.dimensions.smallSpacing)
            .background(AppTheme.colors.backgroundMuted)
    ) {
        Column(
            modifier = Modifier
                .weight(1F)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SmallTagIcon(
                icon = StackedIcon.SmallTag(
                    ImageResource.Local(icon, size = 55.dp),
                    Icons.Filled.Alert.withTint(AppTheme.colors.error)
                ),
                mainIconSize = 88.dp,
                tagIconSize = 44.dp,
                iconBackground = AppTheme.colors.background,
                borderColor = AppTheme.colors.backgroundMuted
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth(),
                text = descriptionText ?: stringResource(description),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppTheme.dimensions.smallSpacing),
            text = stringResource(ctaText),
            onClick = ctaAction
        )
        if (secondaryText != null && secondaryAction != null) {
            MinimalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppTheme.dimensions.smallSpacing),
                text = stringResource(secondaryText),
                onClick = secondaryAction
            )
        }
    }
}

@Preview
@Composable
fun PreviewCustomEmptyState() {
    CustomEmptyState(
        icon = Icons.Filled.User.id,
        ctaAction = {}
    )
}
