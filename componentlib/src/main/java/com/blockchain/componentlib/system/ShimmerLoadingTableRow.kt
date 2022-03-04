package com.blockchain.componentlib.system

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey200
import com.blockchain.componentlib.theme.Grey600

@Composable
fun ShimmerLoadingTableRow() {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(

            /*
             Tween Animates between values over specified [durationMillis]
            */
            tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = listOf(Grey600, Grey200, Grey600),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    ShimmerRow(brush = brush)
}

@Composable
fun ShimmerRow(
    brush: Brush
) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)) {
        ShimmerLargeBlock(brush = brush)
        ShimmerSmallBlock(brush = brush)
    }
}

@Composable
fun ShimmerLargeBlock(brush: Brush) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .weight(3f, true)
                .width(200.dp)
                .height(dimensionResource(R.dimen.xlarge_margin))
                .padding(
                    vertical = dimensionResource(R.dimen.tiny_margin),
                    horizontal = dimensionResource(R.dimen.medium_margin)
                )
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .weight(1f, true)
                .width(45.dp)
                .height(dimensionResource(R.dimen.xlarge_margin))
                .padding(vertical = dimensionResource(R.dimen.tiny_margin))
                .background(brush = brush)
        )
    }
}

@Composable
fun ShimmerSmallBlock(brush: Brush) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .weight(3f, true)
                .height(dimensionResource(R.dimen.standard_margin))
                .padding(
                    vertical = dimensionResource(R.dimen.tiny_margin),
                    horizontal = dimensionResource(R.dimen.medium_margin)
                )
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .weight(1f, true)
                .height(dimensionResource(R.dimen.standard_margin))
                .padding(vertical = dimensionResource(R.dimen.tiny_margin))
                .background(brush = brush)
        )
    }
}

@Composable
@Preview
fun Shimmer_Block() {
    AppTheme {
        AppSurface {
            ShimmerLoadingTableRow()
        }
    }
}
