package com.blockchain.componentlib.control

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.SwitchBg
import com.blockchain.componentlib.theme.SwitchThumbStroke

@Composable
fun Switch(
    isChecked: Boolean = false,
    onCheckChanged: ((Boolean) -> Unit)
) {
    var checked by remember { mutableStateOf(isChecked) }

    val width = 51.dp
    val height = 31.dp
    val gapBetweenThumbAndTrackEdge = 2.dp

    val thumbRadius = (height / 2) - gapBetweenThumbAndTrackEdge

    val animatePosition = animateFloatAsState(
        targetValue = if (checked)
            with(LocalDensity.current) { (width - thumbRadius - gapBetweenThumbAndTrackEdge).toPx() }
        else
            with(LocalDensity.current) { (thumbRadius + gapBetweenThumbAndTrackEdge).toPx() }
    )

    Canvas(
        modifier = Modifier
            .size(width = width, height = height)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        checked = !checked
                        onCheckChanged(checked)
                    }
                )
            }
    ) {
        // Track
        drawRoundRect(
            color = if (checked) Blue600 else SwitchBg,
            cornerRadius = CornerRadius(x = 20.dp.toPx(), y = 20.dp.toPx()),
        )

        // Thumb
        drawCircle(
            color = Color.White,
            radius = thumbRadius.toPx(),
            center = Offset(
                x = animatePosition.value,
                y = size.height / 2
            )
        )

        // outside stroke
        drawCircle(
            color = if (checked) Color.White else SwitchThumbStroke,
            radius = thumbRadius.toPx(),
            center = Offset(
                x = animatePosition.value,
                y = size.height / 2
            ),
            style = Stroke(width = 0.5.dp.toPx())
        )
    }
}

@Preview(name = "Switch Checked")
@Composable
private fun PreviewSwitchChecked() {
    Switch(isChecked = true, onCheckChanged = {})
}

@Preview(name = "Switch Unchecked")
@Composable
private fun PreviewSwitchUnChecked() {
    Switch(isChecked = false, onCheckChanged = {})
}
