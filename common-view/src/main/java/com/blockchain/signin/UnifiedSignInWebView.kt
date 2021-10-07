package com.blockchain.signin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import com.blockchain.common_view.BuildConfig
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
class UnifiedSignInWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private lateinit var listener: UnifiedSignInEventListener

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webViewClient = UnifiedSignInClient(object : UnifiedSignInListener {
            override fun pageFinishedLoading() {
                if (::listener.isInitialized) {
                    listener.onLoaded()
                } else {
                    Timber.e("Listener for webview not initialised")
                }
            }

            override fun onError(error: String) {
                if (::listener.isInitialized) {
                    listener.onError(error)
                } else {
                    Timber.e("Listener for webview not initialised")
                }
            }
        })

        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        addJavascriptInterface(UnifiedSignInInterfaceHandler(), WEB_INTERFACE_NAME)
    }

    fun initWebView(listener: UnifiedSignInEventListener) {
        this.listener = listener
        // TODO should this URL be configurable?
        loadUrl(BuildConfig.WEB_WALLET_URL)
    }

    // FIXME is this needed?
    fun sendMessage() {
        sendMessage("ping")
    }

    // TODO JSON or String? contract is not finalised
    private fun WebView.sendMessage(json: String) {
        evaluateJavascript(WEB_SEND_MESSAGE_CALL.replace(REPLACEABLE_CONTENT, json)) { responseMessage ->
            // TODO only print for now
            Timber.e("Call back from sending web message: $responseMessage")
        }
    }

    companion object {
        private const val WEB_INTERFACE_NAME = "BCAndroidSSI"
        private const val REPLACEABLE_CONTENT = "$$$"
        private const val WEB_SEND_MESSAGE_CALL = "javascript: receiveMessage(\"$REPLACEABLE_CONTENT\")"
    }
}

interface UnifiedSignInEventListener {
    fun onLoaded()
    fun onError(error: String)
}