package com.blockchain.componentlib.alert

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircleIndicator(
    modifier: Modifier = Modifier,
    size: Dp,
    color: Color,
) {
    Canvas(
        modifier = modifier
            .size(size)
    ) {
        drawCircle(
            color = color,
            radius = this@Canvas.size.minDimension / 2,
            center = Offset(this@Canvas.size.width / 2, this@Canvas.size.height / 2)
        )
    }
}

@Preview
@Composable
private fun PreviewCircleIndicator() {
    CircleIndicator(
        size = 8.dp,
        color = Color.Red,
    )
}