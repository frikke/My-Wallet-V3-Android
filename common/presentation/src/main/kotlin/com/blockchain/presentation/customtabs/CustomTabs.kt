package com.blockchain.presentation.customtabs

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun Context.openUrlCustomTabs(
    url: String
) {
    CustomTabsIntent.Builder()
        .build()
        .run {
            launchUrl(this@openUrlCustomTabs, Uri.parse(url))
        }
}