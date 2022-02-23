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

    /**
     * [filterColorId] controls the colour of the background circle
     * [tintColorId] controls the fill colour of the given icon
     */
    class LocalWithBackground(
        @DrawableRes val id: Int,
        @ColorRes val filterColorId: Int,
        @ColorRes val tintColorId: Int,
        val alpha: Float = 0.15F,
        contentDescription: String? = null
    ) : ImageResource(contentDescription)

    class LocalWithBackgroundAndExternalFilterResources(
        @DrawableRes val id: Int,
        val filterColor: String,
        val tintColor: String,
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
