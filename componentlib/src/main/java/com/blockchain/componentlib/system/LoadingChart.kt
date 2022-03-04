package com.blockchain.componentlib.system

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import kotlin.random.Random

@Composable
fun LoadingChart(
    historicalRates: List<SparkLineHistoricalRate>,
    loadingText: String,
) {
    if (historicalRates.isEmpty()) {
        return
    }

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2300, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = listOf(Grey100, Color.White, Grey100),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(AppTheme.colors.background)
    ) {
        Image(
            imageResource = ImageResource.Local(R.drawable.ic_blockchain),
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.CenterHorizontally)
                .padding(top = dimensionResource(R.dimen.medium_margin))
        )
        Text(
            text = loadingText,
            style = AppTheme.typography.micro,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.CenterHorizontally)
                .padding(top = dimensionResource(R.dimen.smallest_margin)),
            color = AppTheme.colors.body
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = dimensionResource(R.dimen.medium_margin))
        ) {
            val width = this.size.width
            val interval = width / historicalRates.size
            var currentX = interval

            val path = Path()

            path.moveTo(0f, historicalRates.first().rate.toFloat())

            historicalRates.forEachIndexed { index, rateEntry ->
                if (index == 0) {
                    return@forEachIndexed
                }
                val previousX = currentX
                val previousY = historicalRates[index - 1].rate.toFloat()

                path.quadraticBezierTo(previousX, previousY, currentX, rateEntry.rate.toFloat())
                currentX += interval
            }

            drawPath(
                path = path,
                brush = brush,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Preview
@Composable
private fun LineLoadingPreview() {
    val data: List<SparkLineHistoricalRate> = List(20) {
        object : SparkLineHistoricalRate {
            override val timestamp: Long = it.toLong()
            override val rate: Double = Random.nextDouble(50.0, 150.0)
        }
    }

    AppTheme {
        AppSurface {
            LoadingChart(
                loadingText = "Loading chart",
                historicalRates = data
            )
        }
    }
}
