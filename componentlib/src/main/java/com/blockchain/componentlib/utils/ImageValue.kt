package com.blockchain.componentlib.utils

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

sealed interface ImageValue {
    data class Local(@DrawableRes val res: Int, val tint: Color? = null) : ImageValue
    data class Remote(val url: String) : ImageValue
}

