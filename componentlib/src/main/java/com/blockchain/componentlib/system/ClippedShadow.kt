package com.blockchain.componentlib.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp

/**
 * Fix for drawing a shadow with a transparent content
 */
@Composable
fun ClippedShadow(
    modifier: Modifier = Modifier,
    elevation: Dp,
    shape: Shape,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Layout(
            Modifier
                .matchParentSize()
                .background(backgroundColor, shape)
                .drawWithCache {
                    // Naive cache setup similar to foundation's Background.
                    val path = Path()
                    var lastSize: Size? = null

                    fun updatePathIfNeeded() {
                        if (size != lastSize) {
                            path.reset()
                            path.addOutline(
                                shape.createOutline(size, layoutDirection, this)
                            )
                            lastSize = size
                        }
                    }

                    onDrawWithContent {
                        updatePathIfNeeded()
                        clipPath(path, ClipOp.Difference) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
                .shadow(elevation, shape)
        ) { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        }

        content()
    }
}
