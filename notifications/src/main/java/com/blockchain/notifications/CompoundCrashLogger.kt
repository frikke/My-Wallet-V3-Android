package com.blockchain.notifications

import com.blockchain.logging.CrashLogger

class CompoundCrashLogger(
    private val loggers: List<CrashLogger>
) : CrashLogger {

    override val isDebugBuild: Boolean
        get() = BuildConfig.DEBUG

    override fun init(ctx: Any) {
        loggers.forEach { it.init(ctx) }
    }

    override fun logEvent(msg: String) {
        loggers.forEach { it.logEvent(msg) }
    }

    override fun logState(name: String, data: String) {
        loggers.forEach { it.logState(name, data) }
    }

    override fun logException(throwable: Throwable, logMsg: String) {
        loggers.forEach { it.logException(throwable, logMsg) }
    }

    override fun logAndRethrowException(throwable: Throwable, logMsg: String) {
        loggers.forEach { it.logException(throwable, logMsg) }
        throw throwable
    }

    override fun onlineState(isOnline: Boolean) {
        loggers.forEach { it.onlineState(isOnline) }
    }

    override fun userLanguageLocale(locale: String) {
        loggers.forEach { it.userLanguageLocale(locale) }
    }
}
