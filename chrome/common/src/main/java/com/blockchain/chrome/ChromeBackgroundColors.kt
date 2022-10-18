package com.blockchain.chrome

import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING

sealed interface ChromeBackgroundColors {
    val startColor: Color
    val endColor: Color

    fun asList() = listOf(startColor, endColor)

    object Trading : ChromeBackgroundColors {
        override val startColor: Color get() = START_TRADING
        override val endColor: Color get() = END_TRADING
    }

    object DeFi : ChromeBackgroundColors {
        override val startColor: Color get() = START_DEFI
        override val endColor: Color get() = END_DEFI
    }
}
