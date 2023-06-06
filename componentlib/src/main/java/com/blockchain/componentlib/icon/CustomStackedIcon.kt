package com.blockchain.componentlib.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun CustomStackedIcon(
    icon: StackedIcon,
    iconBackground: Color = AppTheme.colors.background,
    borderColor: Color = AppTheme.colors.backgroundSecondary,
    size: Dp = AppTheme.dimensions.standardSpacing,
    iconShape: Shape = CircleShape,
    alphaProvider: () -> Float = { 1F }
) {
    when (icon) {
        is StackedIcon.OverlappingPair -> {
            OverlapIcon(
                icon = icon,
                iconSize = size * 0.75f,
                iconBackground = iconBackground,
                borderColor = borderColor
            )
        }
        is StackedIcon.SmallTag -> {
            SmallTagIcon(
                icon = icon,
                mainIconSize = size,
                iconBackground = iconBackground,
                borderColor = borderColor,
                mainIconShape = iconShape
            )
        }
        is StackedIcon.SingleIcon -> {
            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = alphaProvider()
                    }
                    .size(size),
                shape = iconShape,
                color = iconBackground
            ) {
                AsyncMediaItem(
                    modifier = Modifier
                        .size(size),
                    imageResource = icon.icon,
                    onErrorDrawable = com.blockchain.componentlib.icons.R.drawable.coins_on
                )
            }
        }
        StackedIcon.None -> {
            // n/a
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomStackedIconSmallTag() {
    CustomStackedIcon(
        icon = StackedIcon.SmallTag(
            main = ImageResource.Local(R.drawable.ic_close_circle_dark),
            tag = ImageResource.Local(R.drawable.ic_close_circle)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomStackedIconOverlapIcon() {
    CustomStackedIcon(
        icon = StackedIcon.OverlappingPair(
            front = ImageResource.Local(R.drawable.ic_close_circle_dark),
            back = ImageResource.Local(R.drawable.ic_close_circle)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomStackedIconSingleIcon() {
    CustomStackedIcon(
        icon = StackedIcon.SingleIcon(ImageResource.Local(R.drawable.ic_close_circle_dark))
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomStackedIconSingleIconRemote() {
    CustomStackedIcon(
        icon = StackedIcon.SingleIcon(ImageResource.Remote(""))
    )
}
