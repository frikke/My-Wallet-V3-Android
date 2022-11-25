package com.blockchain.logging.data

import android.content.Context
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.embrace.android.embracesdk.Embrace

class EmbraceRemoteLogger(
    private val environmentConfig: EnvironmentConfig
) : RemoteLogger {

    private val embrace
        get() = Embrace.getInstance()

    private var context: Context? = null

    override val isDebugBuild: Boolean
        get() = environmentConfig.isRunningInDebugMode()

    override fun init(context: Any) {
        if (context is Context) {
            this.context = context
            checkEmbrace()
        } else {
            throw IllegalStateException("Unable to init Embrace. No context provided.")
        }
    }

    override fun logUserId(userId: String) {
        checkEmbrace {
            embrace.setUserIdentifier(userId)
        }
    }

    override fun logEvent(message: String) {
        checkEmbrace {
            embrace.logBreadcrumb(message)
        }
    }

    override fun logState(name: String, data: String) {
        checkEmbrace {
            embrace.addSessionProperty(name, data, false)
        }
    }

    override fun onlineState(isOnline: Boolean) {
    }

    override fun userLanguageLocale(locale: String) {
        checkEmbrace {
            embrace.addSessionProperty("locale", locale, false)
        }
    }

    override fun logView(viewName: String) {
        checkEmbrace {
            embrace.logBreadcrumb("VIEW = $viewName")
        }
    }

    override fun logException(throwable: Throwable, logMessage: String) {
        checkEmbrace {
            embrace.logError(throwable, logMessage.ifEmpty { throwable.message }.orEmpty(), emptyMap(), true)
        }
    }

    override fun logAndRethrowException(throwable: Throwable, logMessage: String) {
        logException(throwable, logMessage)
        throw throwable
    }

    private fun checkEmbrace(onEmbraceInitialized: (() -> Unit)? = null) {
        if (embrace.isStarted) {
            onEmbraceInitialized?.invoke()
        } else {
            context?.let { ctx ->
                embrace.start(ctx, isDebugBuild)
                onEmbraceInitialized?.invoke()
            }
        }
    }
}
