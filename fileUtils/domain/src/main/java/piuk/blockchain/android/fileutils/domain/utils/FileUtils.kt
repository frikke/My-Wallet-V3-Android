package piuk.blockchain.android.fileutils.domain.utils

import android.webkit.MimeTypeMap
import java.io.File

fun getMimeType(file: File) =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(
        MimeTypeMap.getFileExtensionFromUrl(file.name).let {
            if (it.isNullOrEmpty()) "pdf" else it
        }
    )
