package com.blockchain.componentlib.system

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100

@Composable
fun ShimmerLoadingTableRow(
    showIconLoader: Boolean = true
) {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = listOf(Grey100, Color.White, Grey100),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (showIconLoader) {
            ShimmerIcon(brush = brush)
        }
        ShimmerRow(brush = brush)
    }
}

@Composable
fun ShimmerIcon(
    brush: Brush
) {
    Box(
        modifier = Modifier
            .padding(
                start = dimensionResource(R.dimen.very_small_spacing),
                top = dimensionResource(R.dimen.large_spacing),
                end = dimensionResource(R.dimen.smallest_spacing)
            )
            .background(
                brush = brush,
                shape = CircleShape
            )
            .size(dimensionResource(R.dimen.standard_spacing))
    )
}

@Composable
fun ShimmerRow(
    brush: Brush
) {
    Column(
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.medium_spacing),
            top = dimensionResource(R.dimen.standard_spacing),
            end = dimensionResource(R.dimen.standard_spacing),
            bottom = dimensionResource(R.dimen.standard_spacing)
        )
    ) {
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
                .weight(3f)
                .height(dimensionResource(R.dimen.standard_spacing))
                .padding(
                    vertical = dimensionResource(R.dimen.smallest_spacing),
                )
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .weight(2f)
        )

        Spacer(
            modifier = Modifier
                .weight(2f)
                .height(dimensionResource(R.dimen.standard_spacing))
                .padding(vertical = dimensionResource(R.dimen.smallest_spacing))
                .background(brush = brush)
        )
    }
}

@Composable
fun ShimmerSmallBlock(brush: Brush) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .weight(2f)
                .height(dimensionResource(R.dimen.medium_spacing))
                .padding(
                    vertical = dimensionResource(R.dimen.smallest_spacing),
                )
                .background(brush = brush)
        )

        Spacer(
            modifier = Modifier
                .weight(3f)
        )

        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(dimensionResource(R.dimen.medium_spacing))
                .padding(vertical = dimensionResource(R.dimen.smallest_spacing))
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

@Composable
@Preview
fun Shimmer_Block_NoIcon() {
    AppTheme {
        AppSurface {
            ShimmerLoadingTableRow(
                showIconLoader = false
            )
        }
    }
}
