package com.blockchain.logging.data

import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import timber.log.Timber

class CompoundRemoteLogger(
    private val remoteLoggers: List<RemoteLogger>,
    private val environmentConfig: EnvironmentConfig,
) : RemoteLogger {

    override val isDebugBuild: Boolean
        get() = environmentConfig.isRunningInDebugMode()

    override fun init(context: Any) {
        remoteLoggers.forEach { it.init(context) }
    }

    override fun logUserId(userId: String) {
        remoteLoggers.forEach { it.logUserId(userId) }
    }

    override fun logEvent(message: String) {
        Timber.i("Logging event: $message")
        remoteLoggers.forEach { it.logEvent(message) }
    }

    override fun logState(name: String, data: String) {
        Timber.i("Logging state: $name ($data)")
        remoteLoggers.forEach { it.logState(name, data) }
    }

    override fun logException(throwable: Throwable, logMessage: String) {
        Timber.e(throwable, logMessage)
        remoteLoggers.forEach { it.logException(throwable, logMessage) }
    }

    override fun logAndRethrowException(throwable: Throwable, logMessage: String) {
        remoteLoggers.forEach { it.logException(throwable, logMessage) }
        throw throwable
    }

    override fun onlineState(isOnline: Boolean) {
        remoteLoggers.forEach { it.onlineState(isOnline) }
    }

    override fun userLanguageLocale(locale: String) {
        remoteLoggers.forEach { it.userLanguageLocale(locale) }
    }

    override fun logView(viewName: String) {
        Timber.i("Logging view: $viewName")
        remoteLoggers.forEach { it.logView(viewName) }
    }
}
