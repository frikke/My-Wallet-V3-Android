package com.blockchain.componentlib.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import timber.log.Timber

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

// url
fun Context.openUrl(url: String) {
    openUrl(Uri.parse(url))
}

fun Context.openUrl(url: Uri) {
    try {
        CustomTabsIntent.Builder()
            .build()
            .run {
                launchUrl(this@openUrl, url)
            }
    } catch (e: Exception) {
        Timber.e("Cannot open url, $e")
    }
}

fun Context.checkValidUrlAndOpen(url: Uri) {
    if (URLUtil.isHttpsUrl(url.toString())) {
        openUrl(url)
    }
}

@Composable
fun OpenUrl(
    url: String
) {
    LocalContext.current.openUrl(url = url)
}

// share
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

// email client
fun Context.openEmailClient() {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_APP_EMAIL)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(
        Intent.createChooser(intent, getString(com.blockchain.stringResources.R.string.security_centre_email_check))
    )
}