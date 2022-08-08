package piuk.blockchain.android.util

import android.content.Context
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import piuk.blockchain.android.R

fun Context.loadRemoteErrorAndStatusIcons(
    iconUrl: String,
    statusIconUrl: String,
    onIconLoadSuccess: (Drawable) -> Unit,
    onIconLoadError: () -> Unit,
    onStatusIconLoadSuccess: (Drawable) -> Unit,
    onStatusIconLoadError: () -> Unit
) {
    val imageLoader = getSvgImageLoader()
    val iconRequest = getErrorIconRequest(
        iconUrl = iconUrl,
        onIconLoadSuccess = onIconLoadSuccess,
        onIconLoadError = onIconLoadError
    )

    val statusIconRequest = ImageRequest.Builder(this)
        .data(statusIconUrl)
        .size(
            resources.getDimension(R.dimen.standard_margin).toInt(),
            resources.getDimension(R.dimen.standard_margin).toInt()
        )
        .target(
            onSuccess = { drawable ->
                onStatusIconLoadSuccess(drawable)
            },
            onError = {
                onStatusIconLoadError()
            }
        )
        .build()

    imageLoader.enqueue(iconRequest)
    imageLoader.enqueue(statusIconRequest)
}

fun Context.loadRemoteErrorIcon(
    iconUrl: String,
    onIconLoadSuccess: (Drawable) -> Unit,
    onIconLoadError: () -> Unit
) {
    val imageLoader = getSvgImageLoader()
    val request = getErrorIconRequest(
        iconUrl = iconUrl,
        onIconLoadSuccess = onIconLoadSuccess,
        onIconLoadError = onIconLoadError
    )
    imageLoader.enqueue(request)
}

private fun Context.getErrorIconRequest(
    iconUrl: String,
    onIconLoadSuccess: (Drawable) -> Unit,
    onIconLoadError: () -> Unit,
) = ImageRequest.Builder(this)
    .data(iconUrl)
    .size(
        resources.getDimension(R.dimen.asset_icon_size_large).toInt(),
        resources.getDimension(R.dimen.asset_icon_size_large).toInt()
    )
    .target(
        onSuccess = { drawable ->
            onIconLoadSuccess(drawable)
        },
        onError = {
            onIconLoadError()
        }
    )
    .build()

private fun Context.getSvgImageLoader(): ImageLoader =
    ImageLoader.Builder(this)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
