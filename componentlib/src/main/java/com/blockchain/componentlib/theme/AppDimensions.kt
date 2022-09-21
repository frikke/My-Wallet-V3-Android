package com.blockchain.componentlib.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.R

class AppDimensions {

    val borderSmall: Dp
        @Composable
        get() = dimensionResource(id = R.dimen.compose_border_small_spacing)

    val composeSmallestSpacing: Dp
        @Composable
        get() = dimensionResource(id = R.dimen.compose_smallest_spacing)

    val smallestSpacing: Dp
        @Composable
        get() = dimensionResource(R.dimen.smallest_spacing)

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
}

internal val LocalDimensions = staticCompositionLocalOf { AppDimensions() }
