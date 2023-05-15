package piuk.blockchain.android.ui.kyc.commonui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White

@Composable
fun ColumnScope.UserIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    @DrawableRes statusIconRes: Int? = null
) {
    ConstraintLayout(modifier) {
        val (userIconRef, statusIconRef) = createRefs()

        Image(
            modifier = Modifier.constrainAs(userIconRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            imageResource = ImageResource.LocalWithBackground(
                id = iconRes,
                iconColor = White,
                backgroundColor = AppTheme.colors.primary,
                alpha = 1f,
                size = AppTheme.dimensions.epicSpacing,
                iconSize = AppTheme.dimensions.hugeSpacing,
                shape = RoundedCornerShape(24.dp)
            )
        )

        if (statusIconRes != null) {
            Image(
                modifier = Modifier.constrainAs(statusIconRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.top)
                    start.linkTo(parent.end)
                    end.linkTo(parent.end)
                },
                imageResource = ImageResource.Local(
                    id = statusIconRes,
                    size = AppTheme.dimensions.hugeSpacing
                )
            )
        }
    }
}
