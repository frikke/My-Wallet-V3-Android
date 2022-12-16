package com.blockchain.componentlib.basic

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

sealed class ImageResource {
    abstract val contentDescription: String?
    abstract val shape: Shape?
    abstract val size: Dp?

    data class Local(
        @DrawableRes val id: Int,
        override val contentDescription: String? = null,
        val colorFilter: ColorFilter? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
    ) : ImageResource() {

        fun withTint(tint: Color) = Local(
            id = id,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            size = size
        )

        fun withSize(size: Dp) = Local(
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

    class LocalWithResolvedDrawable(
        val drawable: Drawable,
        override val contentDescription: String? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
    ) : ImageResource()

    class LocalWithBackground(
        @DrawableRes val id: Int,
        val iconColorFilter: ColorFilter,
        val backgroundColor: Color,
        val alpha: Float = 0.15F,
        override val contentDescription: String? = null,
        override val shape: Shape? = null,
        override val size: Dp? = null,
        val iconSize: Dp? = null
    ) : ImageResource() {
        constructor(
            id: Int,
            iconColor: Color,
            backgroundColor: Color,
            alpha: Float = 0.15F,
            contentDescription: String? = null,
            shape: Shape? = null,
            size: Dp? = null,
            iconSize: Dp? = null
        ) : this(
            id,
            ColorFilter.tint(iconColor),
            backgroundColor,
            alpha,
            contentDescription,
            shape,
            size,
            iconSize,
        )
    }

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
