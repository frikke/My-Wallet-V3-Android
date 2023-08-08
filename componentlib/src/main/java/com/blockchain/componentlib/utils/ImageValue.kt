package com.blockchain.componentlib.utils

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

sealed interface ImageValue {
    data class Local(@DrawableRes val res: Int, val tint: Color? = null) : ImageValue {
        override fun equals(other: Any?): Boolean {
            return other is ImageValue.Local &&
                other.res == res &&
                other.tint?.value == tint?.value
        }

        override fun hashCode(): Int {
            var result = res
            result = 31 * result + (tint?.hashCode() ?: 0)
            return result
        }
    }
    data class Remote(val url: String) : ImageValue
}
