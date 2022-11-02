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
fun SmallTagIcons(
    main: ImageResource,
    tag: ImageResource,
    borderColor: Color = AppTheme.colors.light
) {
    val borderSize = AppTheme.dimensions.composeSmallestSpacing
    val mainIconSize = AppTheme.dimensions.standardSpacing
    val tagIconSize = AppTheme.dimensions.verySmallSpacing
    val overlap = 10.dp

    Box(
        modifier = Modifier
            .size(mainIconSize + tagIconSize - overlap + borderSize)
    ) {

        Image(
            modifier = Modifier
                .size(mainIconSize)
                .background(color = borderColor, shape = CircleShape)
                .border(width = AppTheme.dimensions.noSpacing, Color.Transparent, shape = CircleShape),
            imageResource = main
        )

        Image(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(tagIconSize + borderSize * 2)
                .background(color = borderColor, shape = CircleShape)
                .border(width = borderSize, borderColor, CircleShape)
                .padding(borderSize),
            imageResource = tag
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewSmallTagIcons() {
    SmallTagIcons(
        main = ImageResource.Local(R.drawable.ic_close_circle_dark),
        tag = ImageResource.Local(R.drawable.ic_close_circle),
        borderColor = AppTheme.colors.light
    )
}
