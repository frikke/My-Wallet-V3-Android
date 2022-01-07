package com.blockchain.componentlib.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable

object AppThemeRippleProvider : RippleTheme {

    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        AppTheme.colors.primary,
        !isSystemInDarkTheme()
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        AppTheme.colors.primary,
        !isSystemInDarkTheme()
    )
}
