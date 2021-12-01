package com.blockchain.componentlib.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Red400

@Composable
fun SparkLine(
    historicalRate: List<SparkLineHistoricalRate> = data,
    modifier: Modifier = Modifier,
) {
    val strokeColor = AppTheme.colors.primary

    Canvas(modifier.background(Color.White)) {

        val height = this.size.height
        val width = this.size.width

        val interval = width / (historicalRate.size + 2)
        var currentX = interval
        val halfHeight = height / 2f

        val gradient = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, strokeColor),
            startX = 0.0f,
            endX = width,
        )

        historicalRate.forEachIndexed { index, rateEntry ->
            val nextRateEntry = historicalRate.getOrNull(index + 1) ?: return@forEachIndexed
            drawLine(
                brush = gradient,
                strokeWidth = Stroke.DefaultMiter,
                start = Offset(currentX, halfHeight + rateEntry.rate.toFloat()),
                end = Offset(currentX + interval, halfHeight + nextRateEntry.rate.toFloat()),
            )
            currentX += interval
        }
    }
}

val data: List<SparkLineHistoricalRate> = List(20) {
    object : SparkLineHistoricalRate {
        override val timestamp: Long = it.toLong()
        override val rate: Double = Math.random() * 16
    }
}

interface SparkLineHistoricalRate {
    val timestamp: Long
    val rate: Double
}

@Preview
@Composable
fun SparkLinePreview() {
    AppTheme {
        AppSurface {
            SparkLine(modifier = Modifier.size(64.dp, 16.dp))
        }
    }
}