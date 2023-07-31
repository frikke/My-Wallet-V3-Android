package com.blockchain.componentlib.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SmallTagIcon(
    modifier: Modifier = Modifier,
    icon: StackedIcon.SmallTag,
    iconBackground: Color = AppTheme.colors.light,
    borderColor: Color = AppTheme.colors.backgroundSecondary,
    mainIconSize: Dp = 24.dp,
    tagIconSize: Dp? = null,
    mainIconShape: Shape = CircleShape,
    alphaProvider: () -> Float = { 1F }
) {
    val borderSize = mainIconSize.div(25f)
    val tagIconSize = tagIconSize ?: mainIconSize.div(1.6f)
    val overlap = mainIconSize.times(.4f)

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = alphaProvider()
            }
            .size(mainIconSize + tagIconSize - overlap + borderSize)
    ) {
        Surface(
            modifier = Modifier
                .size(mainIconSize),
            shape = mainIconShape,
            color = (icon.main as? ImageResource.LocalWithBackground)?.backgroundColor ?: iconBackground
        ) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncMediaItem(
                    imageResource = icon.main
                )
            }
        }

        AsyncMediaItem(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(tagIconSize + borderSize * 2)
                .background(
                    color = (icon.tag as? ImageResource.LocalWithBackground)?.backgroundColor ?: iconBackground,
                    shape = CircleShape
                )
                .border(width = borderSize, borderColor, CircleShape)
                .padding(borderSize),
            imageResource = icon.tag
        )
    }
}

@Composable
fun ScreenStatusIcon(
    modifier: Modifier = Modifier,
    main: ImageResource.Local,
    tag: ImageResource.Local,
    iconBackground: Color = AppColors.backgroundSecondary,
    borderColor: Color = AppColors.background,
) {
    SmallTagIcon(
        modifier = modifier,
        icon = StackedIcon.SmallTag(
            main = main
                .withTint(AppColors.title)
                .withBackground(
                    backgroundColor = iconBackground, iconSize = 58.dp, backgroundSize = 88.dp
                ),
            tag = tag,
        ),
        iconBackground = iconBackground,
        borderColor = borderColor,
        mainIconSize = 88.dp,
        tagIconSize = 44.dp,
    )
}

@Preview(backgroundColor = 0XFFF0F2F7, showBackground = true)
@Composable
fun PreviewSmallTagIcons() {
    SmallTagIcon(
        icon = StackedIcon.SmallTag(
            main = ImageResource.Local(R.drawable.ic_close_circle_dark),
            tag = ImageResource.Local(R.drawable.ic_close_circle)
        ),
        borderColor = AppTheme.colors.light
    )
}
