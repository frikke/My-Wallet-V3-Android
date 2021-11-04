package com.blockchain.signin

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.CountDownTimer
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.blockchain.common_view.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

internal class UnifiedSignInInterfaceHandler(val listener: WebViewComms) {
    @JavascriptInterface
    fun postMessage(data: String?) {
        listener.onMessageReceived(data)
    }
}

internal interface WebViewComms {
    fun onMessageReceived(data: String?)
}

internal interface UnifiedSignInListener {
    fun pageFinishedLoading()
    fun onError(error: Throwable)
}

internal class UnifiedSignInClient(val listener: UnifiedSignInListener) : WebViewClient() {
    private var hasPageLoaded = AtomicBoolean(false)

    private val timer: CountDownTimer = object : CountDownTimer(LOAD_TIMEOUT, LOAD_STEP) {
        override fun onTick(millisUntilFinished: Long) {
            // do nothing
        }

        override fun onFinish() {
            if (!hasPageLoaded.get()) {
                listener.onError(PageLoadTimeoutException())
            }
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (BuildConfig.DEBUG) {
            Timber.e("Test SSL error: $error")
            handler?.proceed()
        } else {
            hasPageLoaded.set(false)
            timer.cancel()
            listener.onError(Throwable(error.toString()))
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        hasPageLoaded.set(true)
        timer.cancel()
        listener.pageFinishedLoading()
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        timer.start()
    }

    companion object {
        private const val LOAD_TIMEOUT = 2 * 60 * 1000L // 2 minutes
        private const val LOAD_STEP = 30 * 1000L
        class PageLoadTimeoutException : Throwable("Timeout when loading webview URL")
    }
}
