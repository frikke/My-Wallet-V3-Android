package com.blockchain.componentlib.system

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
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
    modifier: Modifier = Modifier,
    onPageLoaded: () -> Unit = {}
) {
    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = javaScriptEnabled
                settings.useWideViewPort = useWideViewPort
                settings.loadWithOverviewMode = loadWithOverviewMode

                if (disableScrolling) {
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageLoaded()
                    }

                    @Deprecated("Necessary for backward compatibility")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        clearCache(true)
                        return false
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        clearCache(true)
                        return false
                    }
                }

                loadUrl(url)
            }
        },
        modifier = modifier,
        update = {
            it.loadUrl(url)
        }
    )
}
