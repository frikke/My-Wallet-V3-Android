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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.BaseButtonView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import piuk.blockchain.android.R

@Composable
fun ReferralCard(text: String, onClick: () -> Unit) {
    Box(
        modifier =
        Modifier.fillMaxWidth()
            .background(color = Blue600, shape = AppTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {

        Image(
            modifier = Modifier.align(Alignment.TopEnd),
            painter = painterResource(R.drawable.ic_referral_cta),
            contentDescription = ""
        )

        Column(
            modifier = Modifier
                .padding(
                    top = dimensionResource(R.dimen.standard_margin),
                    bottom = dimensionResource(R.dimen.standard_margin),
                    start = dimensionResource(R.dimen.small_margin)
                )

        ) {
            Text(
                style = AppTheme.typography.caption1,
                color = Color.White,
                text = stringResource(R.string.referral_program),
            )

            Text(
                style = AppTheme.typography.body2,
                color = Color.White,
                text = text,
            )
        }
    }
}

@Preview
@Composable
fun ReferralCtaPreview() {
    AppTheme {
        AppSurface {
            ReferralCard(text = "Invite friends, Get \$30!", { })
        }
    }
}

class ReferralCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {
    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                ReferralCard(
                    onClick = onClick,
                    text = text,
                )
            }
        }
    }
}
