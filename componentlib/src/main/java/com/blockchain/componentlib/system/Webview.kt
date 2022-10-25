package com.blockchain.componentlib.system

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import timber.log.Timber

const val WEBVIEW_JAVASCRIPT_INTERFACE_NAME = "Android"
const val WEBVIEW_JAVASCRIPT_LISTENER =
    """
        javascript: (function() {
            window.parent.addEventListener('message', function(event) {
                Android.receiveMessage(JSON.stringify(event.data));
            });
        })()
    """

@Composable
fun Webview(
    url: String,
    javaScriptEnabled: Boolean = true,
    useWideViewPort: Boolean = true,
    loadWithOverviewMode: Boolean = true,
    disableScrolling: Boolean = false,
    modifier: Modifier = Modifier,
    onPageLoaded: () -> Unit = {},
    urlRedirectHandler: (String?) -> Boolean = { false },
    onWebMessageReceived: (String) -> Unit = {}
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
                        // Inject JS to listen for postMessage
                        view?.loadUrl(WEBVIEW_JAVASCRIPT_LISTENER)
                    }

                    @Deprecated("Necessary for backward compatibility")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        Timber.d("Redirecting to $url")
                        view?.clearCache(true)
                        return urlRedirectHandler(url)
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        Timber.d("Redirecting to $request?.url?.toString()")
                        view?.clearCache(true)
                        return urlRedirectHandler(request?.url?.toString())
                    }
                }

                addJavascriptInterface(
                    WebViewPostMessageListener(onWebMessageReceived),
                    WEBVIEW_JAVASCRIPT_INTERFACE_NAME
                )
                loadUrl(url)
            }
        },
        modifier = modifier,
        update = {
            it.loadUrl(url)
        }
    )
}

class WebViewPostMessageListener(private val onMessageReceived: (String) -> Unit) {
    @JavascriptInterface
    fun receiveMessage(data: String): Boolean {
        Timber.d("Post message received: $data")
        onMessageReceived(data)
        return true
    }
}
