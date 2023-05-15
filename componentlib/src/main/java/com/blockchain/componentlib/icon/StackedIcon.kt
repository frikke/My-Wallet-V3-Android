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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun StackedIcons(
    topImageResource: ImageResource,
    bottomImageResource: ImageResource,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(
            width = dimensionResource(com.blockchain.componentlib.R.dimen.large_spacing),
            height = 40.dp
        )
    ) {
        Image(
            imageResource = bottomImageResource,
            modifier = Modifier
                .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .align(Alignment.BottomEnd)
        )

        Image(
            imageResource = topImageResource,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .border(2.dp, AppTheme.colors.background, shape = CircleShape)
                .align(Alignment.TopStart)
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
                bottomImageResource = ImageResource.Remote("", null)
            )
        }
    }
}
