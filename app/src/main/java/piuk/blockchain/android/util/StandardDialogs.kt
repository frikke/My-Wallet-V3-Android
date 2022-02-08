package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.launchUrlInBrowser(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
}
