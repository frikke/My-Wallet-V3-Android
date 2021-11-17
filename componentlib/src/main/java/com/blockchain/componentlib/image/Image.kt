package com.blockchain.componentlib.image

import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Image(
    imageResource: ImageResource,
    modifier: Modifier = Modifier,
    coilImageBuilderScope: (ImageRequest.Builder.() -> Unit)? = null,
) {
    val placeholderColor = AppTheme.colors.light.toArgb()

    val defaultBuilderScope: ImageRequest.Builder.() -> Unit = {
        crossfade(true)
        placeholder(ColorDrawable(placeholderColor))
    }

    val painter = when (imageResource) {
        is ImageResource.Local -> painterResource(id = imageResource.id)
        is ImageResource.Remote -> rememberImagePainter(
            data = imageResource.url,
            builder = coilImageBuilderScope ?: defaultBuilderScope
        )
        ImageResource.None -> return
    }

    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = imageResource.contentDescription,
        modifier = modifier,
    )
}