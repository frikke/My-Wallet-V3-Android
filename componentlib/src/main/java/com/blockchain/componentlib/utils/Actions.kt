package com.blockchain.componentlib.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.blockchain.stringResources.R

fun Context.copyToClipboard(label: String, text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
        ClipData.newPlainText(label, text).also { clipData ->
            setPrimaryClip(clipData)
        }
    }
}

@Composable
fun CopyText(
    label: String = stringResource(id = com.blockchain.stringResources.R.string.app_name),
    textToCopy: String
) {
    LocalContext.current.copyToClipboard(
        label = label,
        text = textToCopy
    )
}

fun Context.openUrl(url: String) {
    openUrl(Uri.parse(url))
}

fun Context.openUrl(url: Uri) {
    startActivity(Intent(Intent.ACTION_VIEW, url))
}

@Composable
fun OpenUrl(
    url: String
) {
    LocalContext.current.openUrl(url = url)
}

fun Context.shareTextWithSubject(text: String, subject: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

@Composable
fun Share(text: String, subject: String) {
    LocalContext.current.shareTextWithSubject(text = text, subject = subject)
}
