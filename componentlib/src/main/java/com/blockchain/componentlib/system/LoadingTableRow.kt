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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ShimmerLoadingTableRow(
    showIconLoader: Boolean = true,
    showEndBlocks: Boolean = true,
    showBottomBlock: Boolean = true,
    reversed: Boolean = false
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
        colors = listOf(AppTheme.colors.light, AppTheme.colors.backgroundSecondary, AppTheme.colors.light),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides if (reversed) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIconLoader) {
                ShimmerIcon(brush = brush)
            }
            ShimmerRow(brush = brush, showEndBlocks = showEndBlocks, showBottomBlock = showBottomBlock)
        }
    }
}

@Composable
fun ShimmerIcon(
    brush: Brush
) {
    Box(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing
            )
            .background(
                brush = brush,
                shape = CircleShape
            )
            .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
    )
}

@Composable
fun ShimmerRow(
    brush: Brush,
    showEndBlocks: Boolean = true,
    showBottomBlock: Boolean = true
) {
    Column(
        modifier = Modifier.padding(
            start = AppTheme.dimensions.smallSpacing,
            top = AppTheme.dimensions.smallSpacing,
            end = AppTheme.dimensions.smallSpacing,
            bottom = AppTheme.dimensions.smallSpacing
        )
    ) {
        ShimmerLargeBlock(brush = brush, showEndBlock = showEndBlocks)
        if (showBottomBlock) {
            ShimmerSmallBlock(brush = brush, showEndBlock = showEndBlocks)
        }
    }
}

@Composable
fun ShimmerLargeBlock(
    brush: Brush,
    showEndBlock: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .weight(3f)
                .height(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                .padding(
                    vertical = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
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
                .height(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                .padding(vertical = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
                .then(if (showEndBlock) Modifier.background(brush = brush) else Modifier)
        )
    }
}

@Composable
fun ShimmerSmallBlock(
    brush: Brush,
    showEndBlock: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .weight(2f)
                .height(dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing))
                .padding(
                    vertical = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
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
                .height(dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing))
                .padding(vertical = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
                .then(if (showEndBlock) Modifier.background(brush = brush) else Modifier)
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
