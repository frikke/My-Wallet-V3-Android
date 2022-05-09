package com.blockchain.logging.data

import android.content.Context
import com.blockchain.logging.RemoteLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseRemoteLogger : RemoteLogger {

    private val firebaseInstance
        get() = FirebaseCrashlytics.getInstance()

    override val isDebugBuild: Boolean
        get() = BuildConfig.DEBUG

    override fun init(context: Any) {
        if (context is Context) {
            if (BuildConfig.USE_CRASHLYTICS) {
                firebaseInstance.setCrashlyticsCollectionEnabled(true)
            } else {
                firebaseInstance.setCrashlyticsCollectionEnabled(false)
            }
        } else {
            throw IllegalStateException("Unable to init Crashlytics. No context provided")
        }
    }

    override fun logEvent(message: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            firebaseInstance.log(message)
        }
    }

    override fun logState(name: String, data: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            firebaseInstance.setCustomKey(name, data)
        }
    }

    override fun onlineState(isOnline: Boolean) {
        if (BuildConfig.USE_CRASHLYTICS) {
            firebaseInstance.setCustomKey(KEY_ONLINE_STATE, isOnline)
        }
    }

    override fun userLanguageLocale(locale: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            firebaseInstance.setCustomKey(KEY_LOCALE_LANGUAGE, locale)
        }
    }

    override fun logException(throwable: Throwable, logMessage: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            firebaseInstance.recordException(throwable)
        }
    }

    override fun logAndRethrowException(throwable: Throwable, logMessage: String) {
        logException(throwable, logMessage)
        throw throwable
    }

    companion object {
        const val KEY_ONLINE_STATE = "online status"
        const val KEY_LOCALE_LANGUAGE = "user language"
    }
}
