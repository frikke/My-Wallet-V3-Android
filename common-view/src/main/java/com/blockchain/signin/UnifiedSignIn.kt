package com.blockchain.signin

import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.blockchain.common_view.BuildConfig
import timber.log.Timber

internal class UnifiedSignInInterfaceHandler {
    @JavascriptInterface
    fun postMessage(data: String?) {
        // TODO when we know how this will work
        Timber.e("---- received message $data")
    }
}

internal interface UnifiedSignInListener {
    fun pageFinishedLoading()
    fun onError(error: String)
}

internal class UnifiedSignInClient(val listener: UnifiedSignInListener) : WebViewClient() {
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (BuildConfig.DEBUG) {
            Timber.e("Test SSL error: $error")
            handler?.proceed()
        } else {
            listener.onError(error.toString())
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        listener.pageFinishedLoading()
    }
}
