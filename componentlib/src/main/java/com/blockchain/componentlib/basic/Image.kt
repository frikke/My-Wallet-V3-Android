package com.blockchain.componentlib.basic

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Image(
    imageResource: ImageResource,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    coilImageBuilderScope: (ImageRequest.Builder.() -> Unit)? = null,
) {
    val placeholderColor = AppTheme.colors.light.toArgb()

    val defaultBuilderScope: ImageRequest.Builder.() -> Unit = {
        crossfade(true)
        placeholder(ColorDrawable(placeholderColor))
    }

    when (imageResource) {
        is ImageResource.Local ->
            androidx.compose.foundation.Image(
                painter = painterResource(id = imageResource.id),
                contentDescription = imageResource.contentDescription,
                modifier = modifier,
                colorFilter = imageResource.colorFilter,
                contentScale = contentScale,
            )
        is ImageResource.LocalWithResolvedBitmap ->
            androidx.compose.foundation.Image(
                painter = rememberImagePainter(imageResource.bitmap),
                contentDescription = imageResource.contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        is ImageResource.Remote ->
            androidx.compose.foundation.Image(
                painter = rememberImagePainter(
                    data = imageResource.url,
                    builder = coilImageBuilderScope ?: defaultBuilderScope
                ),
                contentDescription = imageResource.contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        is ImageResource.LocalWithBackground -> {
            val filterColor = Color(ContextCompat.getColor(LocalContext.current, imageResource.iconTintColour))
            val tintColor = Color(ContextCompat.getColor(LocalContext.current, imageResource.backgroundColour))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(dimensionResource(R.dimen.large_margin))
            ) {
                Box(
                    modifier = Modifier
                        .alpha(imageResource.alpha)
                        .background(
                            color = tintColor,
                            shape = CircleShape
                        )
                        .size(dimensionResource(R.dimen.large_margin))
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageResource.id),
                    contentDescription = imageResource.contentDescription,
                    modifier = modifier,
                    colorFilter = ColorFilter.tint(filterColor),
                    contentScale = contentScale,
                )
            }
        }
        is ImageResource.LocalWithBackgroundAndExternalResources -> {
            val filterColor = Color(android.graphics.Color.parseColor(imageResource.iconTintColour))
            val tintColor = Color(android.graphics.Color.parseColor(imageResource.backgroundColour))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(dimensionResource(R.dimen.large_margin))
            ) {
                Box(
                    modifier = Modifier
                        .alpha(imageResource.alpha)
                        .background(
                            color = tintColor,
                            shape = CircleShape
                        )
                        .size(dimensionResource(R.dimen.large_margin))
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageResource.id),
                    contentDescription = imageResource.contentDescription,
                    modifier = modifier,
                    colorFilter = ColorFilter.tint(filterColor),
                    contentScale = contentScale,
                )
            }
        }
        ImageResource.None -> return
    }
}

@Preview
@Composable
fun Image_Local_24() {
    AppTheme(darkTheme = true) {
        AppSurface {
            Image(
                imageResource = ImageResource.Local(R.drawable.ic_blockchain, ""),
                Modifier.size(dimensionResource(R.dimen.standard_margin)),
            )
        }
    }
}
