package com.blockchain.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openUrl(url: String) {
    openUrl(Uri.parse(url))
}

fun Context.openUrl(url: Uri) {
    startActivity(Intent(Intent.ACTION_VIEW, url))
}