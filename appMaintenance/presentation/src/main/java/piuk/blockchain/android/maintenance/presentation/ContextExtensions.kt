package piuk.blockchain.android.maintenance.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri

// TODO this file to be moved to proper module

fun Context?.openUrl(url: String) {
    openUrl(Uri.parse(url))
}

fun Context?.openUrl(url: Uri) {
    this?.run { startActivity(Intent(Intent.ACTION_VIEW, url)) }
}
