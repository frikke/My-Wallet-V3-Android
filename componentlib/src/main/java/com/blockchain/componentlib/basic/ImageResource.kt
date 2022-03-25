package com.blockchain.componentlib.basic

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ColorFilter

sealed class ImageResource(
    val contentDescription: String? = null
) {

    class Local(
        @DrawableRes val id: Int,
        contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
    ) : ImageResource(contentDescription) {

        fun withColorFilter(colorFilter: ColorFilter) = Local(
            id = id,
            contentDescription = contentDescription,
            colorFilter = colorFilter
        )
    }

    class LocalWithBackground(
        @DrawableRes val id: Int,
        @ColorRes val iconTintColour: Int,
        @ColorRes val backgroundColour: Int,
        val alpha: Float = 0.15F,
        contentDescription: String? = null
    ) : ImageResource(contentDescription)

    class LocalWithBackgroundAndExternalResources(
        @DrawableRes val id: Int,
        val iconTintColour: String,
        val backgroundColour: String,
        val alpha: Float = 0.15F,
        contentDescription: String? = null
    ) : ImageResource(contentDescription)

    class Remote(
        val url: String,
        contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
    ) : ImageResource(contentDescription)

    object None : ImageResource(null)
}
