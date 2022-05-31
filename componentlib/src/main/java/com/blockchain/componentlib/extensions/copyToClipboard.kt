package com.blockchain.componentlib.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun Context.copyToClipboard(label: String, text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
        ClipData.newPlainText(label, text).also { clipData ->
            setPrimaryClip(clipData)
        }
    }
}
