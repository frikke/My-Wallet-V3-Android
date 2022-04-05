package piuk.blockchain.android

import piuk.blockchain.android.util.AppUtil

class UncaughtExceptionHandler private constructor(val appUtil: AppUtil) : Thread.UncaughtExceptionHandler {

    private val rootHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        appUtil.restartApp()

        // Re-throw the exception so that the system can fail as it normally would, and so that
        // Firebase can log the exception automatically
        rootHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun install(appUtil: AppUtil) {
            UncaughtExceptionHandler(appUtil)
        }
    }
}
