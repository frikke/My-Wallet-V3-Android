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
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun OverlapIcons(
    front: ImageResource,
    back: ImageResource,
    borderColor: Color = AppTheme.colors.light
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
        Image(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(iconSize)
                .background(color = borderColor, shape = CircleShape)
                .border(width = AppTheme.dimensions.noSpacing, Color.Transparent, shape = CircleShape),
            imageResource = back
        )

        Image(
            modifier = Modifier
                .size(iconSize + borderSize * 2)
                .background(color = borderColor, shape = CircleShape)
                .border(width = borderSize, borderColor, CircleShape)
                .padding(borderSize),
            imageResource = front
        )
    }
}

@Preview(backgroundColor = 0XFFF0F2F7, showBackground = true)
@Composable
fun PreviewOverlapIcons() {
    OverlapIcons(
        front = ImageResource.Local(R.drawable.ic_close_circle_dark),
        back = ImageResource.Local(R.drawable.ic_close_circle),
        borderColor = AppTheme.colors.light
    )
}
