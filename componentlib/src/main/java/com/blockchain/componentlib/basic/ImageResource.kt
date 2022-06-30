package com.blockchain.componentlib.basic

import android.graphics.Bitmap
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

sealed class ImageResource {
    abstract val contentDescription: String?
    abstract val shape: Shape?
    abstract val size: Dp?

    class Local(
        @DrawableRes val id: Int,
        override val contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
    ) : ImageResource() {

        fun withColorFilter(colorFilter: ColorFilter) = Local(
            id = id,
            contentDescription = contentDescription,
            colorFilter = colorFilter,
            size = size
        )
    }

    class LocalWithResolvedBitmap(
        val bitmap: Bitmap,
        override val contentDescription: String? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
    ) : ImageResource()

    class LocalWithBackground(
        @DrawableRes val id: Int,
        @ColorRes val iconTintColour: Int,
        @ColorRes val backgroundColour: Int,
        val alpha: Float = 0.15F,
        override val contentDescription: String? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
        val iconSize: Dp? = null
    ) : ImageResource()

    class LocalWithBackgroundAndExternalResources(
        @DrawableRes val id: Int,
        val iconTintColour: String,
        val backgroundColour: String,
        val alpha: Float = 0.15F,
        override val contentDescription: String? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
    ) : ImageResource()

    class Remote(
        val url: String,
        override val contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
    ) : ImageResource()

    object None : ImageResource() {
        override val contentDescription: String? = null
        override val shape: Shape? = null
        override val size: Dp? = null
    }
}
