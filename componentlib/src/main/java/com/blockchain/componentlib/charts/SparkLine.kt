package com.blockchain.componentlib.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SparkLine(
    historicalRate: List<SparkLineHistoricalRate>,
    modifier: Modifier = Modifier,
) {
    if (historicalRate.isEmpty()) {
        return
    }

    // Assumption : Crypto rates are never negative
    val maxRate by remember(historicalRate) {
        mutableStateOf(
            historicalRate.maxByOrNull(SparkLineHistoricalRate::rate) ?: return
        )
    }

    val strokeColor = AppTheme.colors.primary

    Canvas(modifier.background(Color.White)) {

        val height = this.size.height
        val width = this.size.width
        val interval = width / historicalRate.size
        var currentX = interval
        val rateScalerValue = height / maxRate.rate.toFloat()
        val gradient = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, strokeColor),
            startX = 0.0f,
            endX = width / 2f,
        )

        val path = Path()

        path.moveTo(0f, historicalRate.first().rate.toFloat() * rateScalerValue)

        historicalRate.forEachIndexed { index, rateEntry ->
            if (index == 0) {
                return@forEachIndexed
            }
            path.lineTo(currentX, rateEntry.rate.toFloat() * rateScalerValue)
            currentX += interval
        }

        drawPath(
            path = path,
            brush = gradient,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Preview
@Composable
fun SparkLinePreview() {
    val data: List<SparkLineHistoricalRate> = List(20) {
        object : SparkLineHistoricalRate {
            override val timestamp: Long = it.toLong()
            override val rate: Double = Math.random() * 1000
        }
    }

    AppTheme {
        AppSurface {
            SparkLine(
                historicalRate = data,
                modifier = Modifier.size(64.dp, 16.dp)
            )
        }
    }
}