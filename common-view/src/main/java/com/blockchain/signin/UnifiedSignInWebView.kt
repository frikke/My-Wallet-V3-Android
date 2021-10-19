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
    private var payload: String = ""

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

            override fun onError(error: Throwable) {
                if (::listener.isInitialized) {
                    if (error is UnifiedSignInClient.Companion.PageLoadTimeoutException) {
                        listener.onTimeout()
                    } else {
                        listener.onFatalError(error)
                    }
                } else {
                    Timber.e("Listener for webview not initialised")
                }
            }
        })

        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        addJavascriptInterface(UnifiedSignInInterfaceHandler(
            object : WebViewComms {
                override fun onMessageReceived(data: String?) {
                    // TODO this will contain messaging logic when contract is finalised
                    sendMessage(payload)
                    // TODO for now, call complete on message reception, we have the password in memory
                    listener.onAuthComplete()
                }
            }
        ), WEB_INTERFACE_NAME)
    }

    fun initWebView(listener: UnifiedSignInEventListener, url: String, payload: String) {
        this.listener = listener
        this.payload = payload
        loadUrl(url)
    }

    private fun WebView.sendMessage(json: String) {
        evaluateJavascript(WEB_SEND_MESSAGE_CALL.replace(REPLACEABLE_CONTENT, json)) { responseMessage ->
            // TODO only print for now
            Timber.e("Call back from sending web message: $responseMessage")
        }
    }

    companion object {
        private const val WEB_INTERFACE_NAME = "BCAndroidSSI"
        private const val REPLACEABLE_CONTENT = "$$$"
        private const val WEB_SEND_MESSAGE_CALL = "javascript: receiveMessageFromMobile(\"$REPLACEABLE_CONTENT\")"
    }
}

interface UnifiedSignInEventListener {
    fun onLoaded()
    fun onFatalError(error: Throwable)
    fun onTimeout()
    fun onAuthComplete() // TODO we will receive a password and sessionId here
}