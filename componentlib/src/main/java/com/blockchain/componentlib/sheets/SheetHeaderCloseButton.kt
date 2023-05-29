package com.blockchain.componentlib.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons

@Composable
internal fun SheetHeaderCloseButton(
    modifier: Modifier = Modifier,
    onClosePress: () -> Unit = {},
    tint: Color,
    background: Color
) {
    Image(
        imageResource = Icons.Close.withTint(tint).withSize(24.dp),
        modifier = modifier
            .background(color = background, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = { onClosePress() }
            ),
        contentScale = ContentScale.None
    )
}
