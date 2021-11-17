package com.blockchain.componentlib.image

import androidx.annotation.DrawableRes

sealed class ImageResource(
    val contentDescription: String? = null
) {

    class Local(
        @DrawableRes val id: Int,
        contentDescription: String?
    ) : ImageResource(contentDescription)

    class Remote(
        val url: String,
        contentDescription: String?,
    ) : ImageResource(contentDescription)

    object None : ImageResource(null)
}
