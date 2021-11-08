package com.blockchain.signin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import com.blockchain.common_view.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
class UnifiedSignInWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private lateinit var listener: UnifiedSignInEventListener
    private var payload: String = ""
    private val jsonBuilder: Json by lazy {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

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
        addJavascriptInterface(
            UnifiedSignInInterfaceHandler(
                object : WebViewComms {
                    override fun onMessageReceived(data: String?) {
                        data?.let {
                            parsePayload(it)
                        }
                    }
                }
            ),
            WEB_INTERFACE_NAME
        )
    }

    private fun parsePayload(data: String) {
        val webResponse = jsonBuilder.runCatching {
            decodeFromString<WebViewMessaging.Handshake>(data)
        }.getOrElse {
            jsonBuilder.runCatching {
                decodeFromString<WebViewMessaging.MergeResponse>(data)
            }.getOrNull()
        }

        when (webResponse) {
            is WebViewMessaging.Handshake -> {
                val base64Body = WebViewMessaging.PayloadBody(
                    payload = payload
                )
                sendMessage(jsonBuilder.encodeToString(base64Body))
            }
            is WebViewMessaging.MergeResponse -> {
            }
            null -> {
                Timber.e("Web response was neither handshake nor a merge response")
            }
            else -> {
                Timber.e("something weird happened here")
            }
        }
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

internal sealed class WebViewMessaging {
    @Serializable
    data class Handshake(
        @SerialName("status")
        val status: String
    ) : WebViewMessaging()

    @Serializable
    data class PayloadBody(
        @SerialName("payload")
        val payload: String
    ) : WebViewMessaging()

    @Serializable
    data class MergeResponse(
        @SerialName("status")
        val status: String,
        @SerialName("error")
        val error: String?,
        @SerialName("data")
        val data: MergeData?
    )

    @Serializable
    data class MergeData(
        @SerialName("guid")
        val guid: String,
        @SerialName("password")
        val password: String,
        @SerialName("sessionId")
        val sessionId: String
    ) : WebViewMessaging()
}

interface UnifiedSignInEventListener {
    fun onLoaded()
    fun onFatalError(error: Throwable)
    fun onTimeout()
    fun onAuthComplete() // TODO we will receive a password and sessionId here
}
