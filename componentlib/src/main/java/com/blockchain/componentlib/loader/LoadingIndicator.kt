package com.blockchain.componentlib.loader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color,
) {
    val transition = rememberInfiniteTransition(
        label = "LoadingIndicator"
    )
    val currentArcStartAngle by transition.animateValue(
        initialValue = 0,
        targetValue = 360,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        ),
        label = "LoadingIndicator"
    )

    Canvas(
        modifier
            .size(size)
            .padding(2.5.dp / 2)
    ) {
        drawCircle(
            color = color.copy(alpha = 0.25F),
            style = Stroke(width = 2.5.dp.toPx())
        )

        drawArc(
            color = color.copy(alpha = 0.55F),
            startAngle = currentArcStartAngle.toFloat(),
            sweepAngle = 90F,
            useCenter = false,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0XFF07080D)
@Composable
fun ButtonLoadingIndicatorPreview() {
    Column(
        modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator(
            color = Color.Red
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        LoadingIndicator(
            color = Color.Yellow
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        LoadingIndicator(
            color = Color.Green
        )
    }
}
