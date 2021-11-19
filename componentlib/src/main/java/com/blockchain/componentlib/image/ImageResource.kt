package com.blockchain.componentlib.image

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

sealed class ImageResource(
    val contentDescription: String? = null
) {

    class Local(
        @DrawableRes val id: Int,
        contentDescription: String?
    ) : ImageResource(contentDescription)

    class LocalWithBackground(
        @DrawableRes val id: Int,
        @ColorRes val filterColorId: Int,
        @ColorRes val tintColorId: Int,
        val alpha: Float = 0.15F,
        contentDescription: String?
    ) : ImageResource(contentDescription)

    class Remote(
        val url: String,
        contentDescription: String?,
    ) : ImageResource(contentDescription)

    object None : ImageResource(null)
}
