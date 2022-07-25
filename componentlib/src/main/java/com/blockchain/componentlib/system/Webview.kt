package com.blockchain.componentlib.system

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun Webview(
    url: String,
    javaScriptEnabled: Boolean = true,
    useWideViewPort: Boolean = true,
    loadWithOverviewMode: Boolean = true,
    disableScrolling: Boolean = false,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                webViewClient = WebViewClient()

                settings.javaScriptEnabled = javaScriptEnabled
                settings.useWideViewPort = useWideViewPort
                settings.loadWithOverviewMode = loadWithOverviewMode

                if (disableScrolling) {
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
                }

                loadUrl(url)
            }
        },
        modifier = modifier
    )
}
