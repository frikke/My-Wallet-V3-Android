package com.blockchain.componentlib.basic

import android.graphics.Bitmap
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape

sealed class ImageResource(
    val contentDescription: String? = null,
    val shape: Shape? = null
) {

    class Local(
        @DrawableRes val id: Int,
        contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
        shape: Shape? = null,
    ) : ImageResource(contentDescription, shape) {

        fun withColorFilter(colorFilter: ColorFilter) = Local(
            id = id,
            contentDescription = contentDescription,
            colorFilter = colorFilter
        )
    }

    class LocalWithResolvedBitmap(
        val bitmap: Bitmap,
        contentDescription: String? = null,
        shape: Shape? = null,
    ) : ImageResource(contentDescription, shape)

    class LocalWithBackground(
        @DrawableRes val id: Int,
        @ColorRes val iconTintColour: Int,
        @ColorRes val backgroundColour: Int,
        val alpha: Float = 0.15F,
        contentDescription: String? = null,
        shape: Shape? = null,
    ) : ImageResource(contentDescription, shape)

    class LocalWithBackgroundAndExternalResources(
        @DrawableRes val id: Int,
        val iconTintColour: String,
        val backgroundColour: String,
        val alpha: Float = 0.15F,
        contentDescription: String? = null,
        shape: Shape? = null,
    ) : ImageResource(contentDescription, shape)

    class Remote(
        val url: String,
        contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
        shape: Shape? = null,
    ) : ImageResource(contentDescription, shape)

    object None : ImageResource(null)
}
