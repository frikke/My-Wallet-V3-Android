package com.blockchain.componentlib.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey800

@Composable
fun CardCloseButton(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    backgroundColor: Color = if (!isDarkTheme) {
        Grey100
    } else {
        Grey800
    },
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clickable {
                onClick.invoke()
            }
            .size(24.dp)
            .background(color = backgroundColor, shape = CircleShape)
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            imageResource = ImageResource.Local(R.drawable.ic_close, null)
        )
    }
}
