package com.blockchain.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppColors

@Stable
sealed interface ChromeBackgroundColors {
    @get:Composable
    val startColor: Color

    @get:Composable
    val endColor: Color

    @Composable
    fun asList() = listOf(startColor, endColor)

    object Trading : ChromeBackgroundColors {
        override val startColor: Color @Composable get() = AppColors.custodialStartBackground
        override val endColor: Color @Composable get() = AppColors.custodialEndBackground
    }

    object DeFi : ChromeBackgroundColors {
        override val startColor: Color @Composable get() = AppColors.defiStartBackground
        override val endColor: Color @Composable get() = AppColors.defiEndBackground
    }
}
