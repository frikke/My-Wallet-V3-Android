package piuk.blockchain.android.ui.referral.presentation.composable

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.annotation.ExperimentalCoilApi
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.BaseButtonView
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import piuk.blockchain.android.R

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ReferralCard(
    title: String,
    text: String,
    onClick: () -> Unit,
    backgroundResourceUrl: String,
    iconUrl: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, shape = AppTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {

        if (backgroundResourceUrl.isNotEmpty()) {
            AsyncMediaItem(
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .clipToBounds()
                    .matchParentSize(),
                url = backgroundResourceUrl
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = Blue600, shape = AppTheme.shapes.large)
            )
        }

        if (iconUrl.isNotEmpty()) {
            if (backgroundResourceUrl.isNotEmpty()) {
                AsyncMediaItem(
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.align(Alignment.TopEnd),
                    url = iconUrl
                )
            }
        } else {
            Image(
                modifier = Modifier.align(Alignment.TopEnd),
                painter = painterResource(R.drawable.ic_referral_cta),
                contentDescription = null
            )
        }

        Column {
            SimpleText(
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.standard_margin),
                    start = dimensionResource(R.dimen.small_margin)
                ),
                text = title.ifEmpty { stringResource(R.string.referral_program) },
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Light,
                gravity = ComposeGravities.Start
            )

            SimpleText(
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.smallest_margin),
                    bottom = dimensionResource(R.dimen.standard_margin),
                    start = dimensionResource(R.dimen.small_margin)
                ),
                text = text,
                style = ComposeTypographies.Body2,
                color = ComposeColors.Light,
                gravity = ComposeGravities.Start
            )
        }
    }
}

@Preview
@Composable
fun ReferralCtaPreview() {
    AppTheme {
        AppSurface {
            ReferralCard(
                title = "a title",
                text = "Invite friends, Get \$30!",
                onClick = { },
                backgroundResourceUrl = "",
                iconUrl = ""
            )
        }
    }
}

class ReferralCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    var title: String by mutableStateOf("")
    var backgroundResourceUrl: String by mutableStateOf("")
    var iconUrl: String by mutableStateOf("")

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                ReferralCard(
                    title = title,
                    text = text,
                    onClick = onClick,
                    backgroundResourceUrl = backgroundResourceUrl,
                    iconUrl = iconUrl
                )
            }
        }
    }
}
