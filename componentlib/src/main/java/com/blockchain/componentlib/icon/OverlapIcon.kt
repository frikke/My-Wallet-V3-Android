package com.blockchain.componentlib.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun OverlapIcon(
    icon: StackedIcon.OverlappingPair,
    iconBackground: Color = AppTheme.colors.light,
    borderColor: Color = AppTheme.colors.background
) {
    val borderSize = AppTheme.dimensions.composeSmallestSpacing
    val iconSize = 18.dp
    val overlap = 6.dp

    Box(
        modifier = Modifier
            .size(
                height = iconSize * 2 - overlap + borderSize,
                width = iconSize + overlap + borderSize
            )
    ) {
        AsyncMediaItem(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(iconSize)
                .background(color = iconBackground, shape = CircleShape)
                .border(width = AppTheme.dimensions.noSpacing, Color.Transparent, shape = CircleShape),
            imageResource = icon.back
        )

        AsyncMediaItem(
            modifier = Modifier
                .size(iconSize + borderSize * 2)
                .background(color = iconBackground, shape = CircleShape)
                .border(width = borderSize, borderColor, CircleShape)
                .padding(borderSize),
            imageResource = icon.front
        )
    }
}

@Preview(backgroundColor = 0XFFF0F2F7, showBackground = true)
@Composable
fun PreviewOverlapIcons() {
    OverlapIcon(
        icon = StackedIcon.OverlappingPair(
            front = ImageResource.Local(R.drawable.ic_close_circle_dark),
            back = ImageResource.Local(R.drawable.ic_close_circle)
        ),
        borderColor = AppTheme.colors.light
    )
}
