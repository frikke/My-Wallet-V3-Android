package com.blockchain.notifications

import android.content.Context
import com.blockchain.logging.CrashLogger
import io.embrace.android.embracesdk.Embrace

class EmbraceCrashLogger : CrashLogger {

    private val embrace
        get() = Embrace.getInstance()

    override val isDebugBuild: Boolean
        get() = BuildConfig.DEBUG

    override fun init(ctx: Any) {
        if (ctx is Context) {
            embrace.start(ctx, isDebugBuild)
        } else {
            throw IllegalStateException("Unable to init Embrace. No context provided")
        }
    }

    override fun logEvent(msg: String) {
        embrace.logBreadcrumb(msg)
    }

    override fun logState(name: String, data: String) {
        embrace.addSessionProperty(name, data, false)
    }

    override fun onlineState(isOnline: Boolean) {
    }

    override fun userLanguageLocale(locale: String) {
        embrace.addSessionProperty("locale", locale, false)
    }

    override fun logException(throwable: Throwable, logMsg: String) {
        embrace.logError(throwable, logMsg.ifEmpty { throwable.message }.orEmpty(), emptyMap(), true)
    }

    override fun logAndRethrowException(throwable: Throwable, logMsg: String) {
        logException(throwable, logMsg)
        throw throwable
    }
}
