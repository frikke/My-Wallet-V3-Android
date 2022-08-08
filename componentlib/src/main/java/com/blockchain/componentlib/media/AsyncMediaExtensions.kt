package com.blockchain.componentlib.media

import android.webkit.MimeTypeMap

enum class UrlType {
    PNG, JPG, GIF, SVG,
    JSON,
    WAV, MP4, FLV
}

fun String.getUrlType(): String = MimeTypeMap.getFileExtensionFromUrl(this).uppercase()
