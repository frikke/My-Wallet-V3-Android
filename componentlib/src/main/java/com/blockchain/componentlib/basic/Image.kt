package com.blockchain.componentlib.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun Image(
    imageResource: ImageResource,
    modifier: Modifier = Modifier,
    defaultShape: Shape = CircleShape,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit
) {
    when (imageResource) {
        is ImageResource.Local ->
            androidx.compose.foundation.Image(
                alignment = alignment,
                painter = painterResource(id = imageResource.id),
                contentDescription = imageResource.contentDescription,
                modifier = modifier
                    .run { imageResource.size?.let { size(it) } ?: this }
                    .run { imageResource.shape?.let { clip(it) } ?: this },
                colorFilter = imageResource.colorFilter,
                contentScale = contentScale,
            )
        is ImageResource.LocalWithResolvedBitmap ->
            androidx.compose.foundation.Image(
                alignment = alignment,
                painter = rememberAsyncImagePainter(imageResource.bitmap),
                contentDescription = imageResource.contentDescription,
                modifier = modifier
                    .run { imageResource.size?.let { size(it) } ?: size(dimensionResource(R.dimen.large_spacing)) }
                    .run { imageResource.shape?.let { clip(it) } ?: clip(defaultShape) },
                contentScale = contentScale,
            )
        is ImageResource.LocalWithResolvedDrawable ->
            androidx.compose.foundation.Image(
                alignment = alignment,
                painter = rememberDrawablePainter(imageResource.drawable),
                contentDescription = imageResource.contentDescription,
                modifier = imageResource.shape?.let {
                    Modifier
                        .size(dimensionResource(R.dimen.large_spacing))
                        .clip(it)
                } ?: modifier,
                contentScale = contentScale,
            )
        is ImageResource.Remote ->
            androidx.compose.foundation.Image(
                alignment = alignment,
                painter = rememberAsyncImagePainter(imageResource.url),
                contentDescription = imageResource.contentDescription,
                modifier = modifier
                    .run { imageResource.size?.let { size(it) } ?: size(dimensionResource(R.dimen.large_spacing)) }
                    .run { imageResource.shape?.let { clip(it) } ?: clip(defaultShape) },
                contentScale = contentScale
            )
        is ImageResource.LocalWithBackground -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.run {
                    imageResource.size?.let { size(it) } ?: size(dimensionResource(R.dimen.large_spacing))
                }
            ) {
                Box(
                    modifier = Modifier
                        .alpha(imageResource.alpha)
                        .background(
                            color = imageResource.backgroundColor,
                            shape = imageResource.shape ?: defaultShape
                        )
                        .run {
                            imageResource.size?.let { size(it) } ?: size(dimensionResource(R.dimen.large_spacing))
                        }
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageResource.id),
                    contentDescription = imageResource.contentDescription,
                    modifier = modifier.run { imageResource.iconSize?.let { size(it) } ?: this },
                    colorFilter = imageResource.iconColorFilter,
                    contentScale = contentScale,
                )
            }
        }
        is ImageResource.LocalWithBackgroundAndExternalResources -> {
            val filterColor = Color(android.graphics.Color.parseColor(imageResource.iconTintColour))
            val tintColor = Color(android.graphics.Color.parseColor(imageResource.backgroundColour))
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.size(dimensionResource(R.dimen.large_spacing))
            ) {
                Box(
                    modifier = Modifier
                        .alpha(imageResource.alpha)
                        .background(
                            color = tintColor,
                            shape = imageResource.shape ?: defaultShape
                        )
                        .run {
                            imageResource.size?.let { size(it) } ?: size(dimensionResource(R.dimen.large_spacing))
                        }
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(imageResource.id),
                    contentDescription = imageResource.contentDescription,
                    modifier = modifier,
                    alignment = alignment,
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
                modifier = Modifier.size(dimensionResource(R.dimen.standard_spacing)),
            )
        }
    }
}
