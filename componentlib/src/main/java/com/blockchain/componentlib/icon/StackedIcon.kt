package com.blockchain.componentlib.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun StackedIcons(
    topImageResource: ImageResource,
    bottomImageResource: ImageResource,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(width = 32.dp, height = 40.dp)
    ) {
        Image(
            imageResource = bottomImageResource,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .align(Alignment.BottomEnd),
            coilImageBuilderScope = null
        )

        Image(
            imageResource = topImageResource,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .border(2.dp, AppTheme.colors.background, shape = CircleShape)
                .align(Alignment.TopStart),
            coilImageBuilderScope = null
        )
    }
}

@Preview
@Composable
fun StackedIcons_Basic() {
    AppTheme {
        AppSurface {
            StackedIcons(
                topImageResource = ImageResource.Remote("", null),
                bottomImageResource = ImageResource.Remote("", null),
            )
        }
    }
}
