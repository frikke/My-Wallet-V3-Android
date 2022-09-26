@file:OptIn(ExperimentalCoilApi::class)

package com.blockchain.componentlib.media

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.imageLoader
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieRetrySignal
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource.None.contentDescription
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@ExperimentalCoilApi
@Composable
fun AsyncMediaItem(
    modifier: Modifier = Modifier,
    url: String,
    fallbackUrlType: UrlType? = null,
    contentDescription: String? = "async media item",
    contentScale: ContentScale = ContentScale.Fit,
    @DrawableRes onLoadingPlaceholder: Int = R.drawable.bkgd_grey_900_rounded,
    @DrawableRes onErrorDrawable: Int = R.drawable.ic_error
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        when (url.getUrlType().ifEmpty { fallbackUrlType?.name }) {
            UrlType.MP4.name,
            UrlType.WAV.name,
            UrlType.FLV.name -> {
                VideoPlayerItem(
                    modifier = modifier,
                    sourceUrl = url
                )
            }
            UrlType.JSON.name -> {
                val retrySignal = rememberLottieRetrySignal()
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.Url(url),
                    onRetry = { _, _ ->
                        retrySignal.retry()
                        true
                    },
                )

                LottieAnimation(
                    modifier = modifier,
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                )
            }
            UrlType.JPG.name,
            UrlType.PNG.name -> {
                val imageRequest = ImageRequest.Builder(context)
                    .data(url)
                    .placeholder(onLoadingPlaceholder)
                    .error(onErrorDrawable)
                    .crossfade(true)
                    .build()

                context.imageLoader.enqueue(imageRequest)

                AsyncImage(
                    model = imageRequest,
                    modifier = modifier,
                    contentDescription = contentDescription,
                    contentScale = contentScale
                )
            }
            UrlType.SVG.name,
            UrlType.GIF.name -> {
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        add(SvgDecoder.Factory())
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()

                val imageRequest = ImageRequest.Builder(context).data(data = url).build()
                context.imageLoader.enqueue(imageRequest)

                AsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    modifier = modifier,
                    contentDescription = contentDescription,
                    contentScale = contentScale
                )
            }
        }
    }
}

@Preview
@Composable
fun AsyncMediaItem_mp4() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://i.imgur.com/lHL4Xy0.mp4",
                contentDescription = "video"
            )
        }
    }
}

@Preview
@Composable
fun AsyncMediaItem_gif() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://i.stack.imgur.com/IzNGE.gif",
                contentDescription = "gif"
            )
        }
    }
}

@Preview
@Composable
fun AsyncMediaItem_png() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://w7.pngwing.com/pngs/915/345/png-transparent-multicolored-balloons-illustration-" +
                    "balloon-balloon-free-balloons-easter-egg-desktop-wallpaper-party-thumbnail.png",
                contentDescription = "png"
            )
        }
    }
}

@Preview
@Composable
fun AsyncMediaItem_jpg() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://img-19.commentcamarche.net/cI8qqj-finfDcmx6jMK6Vr-krEw=/1500x/" +
                    "smart/b829396acc244fd484c5ddcdcb2b08f3/ccmcms-commentcamarche/20494859.jpg",
                contentDescription = "jpg"
            )
        }
    }
}

@Preview
@Composable
fun AsyncMediaItem_lottie() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://assets2.lottiefiles.com/packages/lf20_q77jpumk.json",
                contentDescription = "lottie"
            )
        }
    }
}

@Preview
@Composable
fun AsyncMediaItem_svg() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://www.svgrepo.com/show/240138/lock.svg",
                contentDescription = "svg"
            )
        }
    }
}
