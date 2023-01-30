package com.blockchain.componentlib.theme

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R

class AppDimensions {

    val borderSmall: Dp
        @Composable
        get() = dimensionResource(id = R.dimen.compose_border_small_spacing)

    val noSpacing: Dp
        get() = 0.dp

    val composeSmallestSpacing: Dp
        @Composable
        get() = dimensionResource(id = R.dimen.compose_smallest_spacing)

    val smallestSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.smallest_spacing)

    val minusculeSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.minuscule_spacing)

    val tinySpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.tiny_spacing)

    val verySmallSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.very_small_spacing)

    val smallSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.small_spacing)

    val mediumSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.medium_spacing)

    val standardSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.standard_spacing)

    val largeSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.large_spacing)

    val xLargeSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.xlarge_spacing)

    val hugeSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.huge_spacing)

    val xHugeSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.xhuge_spacing)

    val epicSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.epic_spacing)

    val borderRadiiSmallest: Dp
        @Composable
        get() = dimensionResource(R.dimen.borderRadiiSmallest)

    val borderRadiiSmall: Dp
        @Composable
        get() = dimensionResource(R.dimen.borderRadiiSmall)

    val borderRadiiMedium: Dp
        @Composable
        get() = dimensionResource(R.dimen.borderRadiiMedium)

    val borderRadiiLarge: Dp
        @Composable
        get() = dimensionResource(R.dimen.borderRadiiLarge)

    val smallElevation: Dp
        @Composable
        get() = dimensionResource(R.dimen.small_elevation)

    val mediumElevation: Dp
        @Composable
        get() = dimensionResource(R.dimen.medium_elevation)
}

// Vertical Spacers
@Composable
fun SmallestVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.smallestSpacing))
}

@Composable
fun TinyVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))
}

@Composable
fun VerySmallVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.verySmallSpacing))
}

@Composable
fun SmallVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
}

@Composable
fun MediumVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.mediumSpacing))
}

@Composable
fun StandardVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))
}

@Composable
fun LargeVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.largeSpacing))
}

@Composable
fun XLargeVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.xLargeSpacing))
}

@Composable
fun HugeVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.hugeSpacing))
}

@Composable
fun EpicVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.dimensions.epicSpacing))
}

// Horizontal Spacers
@Composable
fun SmallestHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.smallestSpacing))
}

@Composable
fun TinyHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.tinySpacing))
}

@Composable
fun VerySmallHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.verySmallSpacing))
}

@Composable
fun SmallHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.smallSpacing))
}

@Composable
fun MediumHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.mediumSpacing))
}

@Composable
fun StandardHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.standardSpacing))
}

@Composable
fun LargeHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.largeSpacing))
}

@Composable
fun XLargeHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.xLargeSpacing))
}

@Composable
fun HugeHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.hugeSpacing))
}

@Composable
fun EpicHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.dimensions.epicSpacing))
}

internal val LocalDimensions = staticCompositionLocalOf { AppDimensions() }
