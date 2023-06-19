package com.blockchain.componentlib.loader

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun TasksIndicator(
    modifier: Modifier = Modifier,
    allTasksCount: Int,
    completedTasksCount: Int,
    size: Dp = 40.dp,
    color: Color,
) {
    require(allTasksCount >= completedTasksCount) { "completedTasksCount cannot be greater than allTasksCount" }

    val stroke = 4.dp

    val circleColor = AppColors.light

    Box(
        modifier = modifier.size(size),
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(stroke / 2)
        ) {
            drawCircle(
                color = circleColor,
                style = Stroke(width = stroke.toPx())
            )

            drawArc(
                color = color,
                startAngle = -90F,
                sweepAngle = 360F / (allTasksCount.toFloat() / completedTasksCount.toFloat()),
                useCenter = false,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
            )
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "$completedTasksCount/$allTasksCount",
            style = AppTheme.typography.paragraph2SlashedZero,
            color = color
        )
    }
}

@Preview
@Composable
private fun PreviewTasksIndicator() {
    TasksIndicator(allTasksCount = 3, completedTasksCount = 1, color = AppColors.primary)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTasksIndicatorDark() {
    PreviewTasksIndicator()
}
