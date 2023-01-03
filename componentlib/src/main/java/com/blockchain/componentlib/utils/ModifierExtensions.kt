package com.blockchain.componentlib.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import kotlin.math.max

fun Modifier.clickableNoEffect(onClick: () -> Unit) = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.clickableNoEffect(onClick: () -> Unit, onLongClick: () -> Unit) = composed {
    combinedClickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
        onLongClick = onLongClick
    )
}

fun Modifier.circleAround(color: Color) = clip(CircleShape)
    .background(color)
    .layout { measurable, constraints ->
        with(measurable.measure(constraints)) {
            val size = max(width, height)

            layout(width = size, height = size) {
                placeRelative(
                    x = size / 2 - width / 2,
                    y = size / 2 - height / 2
                )
            }
        }
    }
