package piuk.blockchain.android.ui.referral.presentation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import com.blockchain.componentlib.viewextensions.gone
import piuk.blockchain.android.R

@Composable
fun ReferralSuccessCard(
    cardTitle: String,
    text: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Blue600, shape = AppTheme.shapes.large),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_spacing)))

        Image(
            imageResource = ImageResource.LocalWithBackground(
                id = R.drawable.ic_present,
                iconColor = AppTheme.colors.primary,
                backgroundColor = White,
                alpha = 1F,
                size = dimensionResource(com.blockchain.componentlib.R.dimen.huge_spacing),
                iconSize = dimensionResource(R.dimen.standard_spacing)
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(dimensionResource(R.dimen.small_spacing))

        ) {
            Text(
                style = AppTheme.typography.caption1,
                color = Grey100,
                text = cardTitle,
            )

            Text(
                style = AppTheme.typography.body2,
                color = Color.White,
                text = text,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Top)
                .padding(dimensionResource(R.dimen.very_small_spacing))
                .clickable {
                    onClose()
                }
                .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                .background(color = colorResource(R.color.faded_white), shape = CircleShape)
        ) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center),
                imageResource = ImageResource.Local(
                    R.drawable.ic_close_referrals, null, ColorFilter.tint(Blue600)
                ),
            )
        }
    }
}

@Preview
@Composable
fun ReferralSuccessCardPreview() {
    AppTheme {
        AppSurface {
            ReferralSuccessCard(
                cardTitle = "Referral Program",
                text = "Congrats!\nYou just received \$30!",
                { }
            )
        }
    }
}

class ReferralSuccessCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var onClose = { this.gone() }

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                ReferralSuccessCard(
                    cardTitle = title,
                    text = subtitle,
                    onClose = onClose
                )
            }
        }
    }
}
