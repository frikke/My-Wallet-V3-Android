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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun StepIndicator(
    modifier: Modifier = Modifier,
    stepsCount: Int,
    completedSteps: Int,
    size: Dp = 40.dp,
    color: Color,
) {
    require(stepsCount >= completedSteps) { "completedSteps cannot be greater than stepsCount" }

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
                sweepAngle = 360F / (stepsCount.toFloat() / completedSteps.toFloat()),
                useCenter = false,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
            )
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "$completedSteps/$stepsCount",
            style = AppTheme.typography.paragraph2SlashedZero,
            color = color
        )
    }
}

@Preview
@Composable
private fun PreviewStepIndicator() {
    StepIndicator(stepsCount = 3, completedSteps = 1, color = AppColors.primary)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewStepIndicatorDark() {
    PreviewStepIndicator()
}
