package com.blockchain.componentlib.control

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import java.lang.Integer.min

@Composable
fun PagerIndicatorDots(
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    count: Int = 2,
) {
    val dotSize = 8.dp
    val dotSpacingSize = 8.dp
    val selectedIndexOffset = (dotSize + dotSpacingSize) * min(selectedIndex, count - 1)

    Box(modifier) {
        Row {
            for (currentIndex in 0 until count) {
                PagerIndicatorDot(dotSize = dotSize)
                if (currentIndex != count - 1) {
                    Spacer(Modifier.width(dotSpacingSize))
                }
            }
        }
        PagerIndicatorDot(
            dotSize = dotSize,
            isSelected = true,
            modifier = Modifier.offset(
                x = animateDpAsState(targetValue = selectedIndexOffset).value,
                y = 0.dp,
            )
        )
    }
}

@Composable
private fun PagerIndicatorDot(
    dotSize: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(dotSize)
            .background(
                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.medium,
                shape = CircleShape
            )
    )
}

@Preview
@Composable
fun PagerIndicatorDotsPreview() {
    AppTheme {
        AppSurface {
            PagerIndicatorDots(selectedIndex = 0, count = 3)
        }
    }
}
