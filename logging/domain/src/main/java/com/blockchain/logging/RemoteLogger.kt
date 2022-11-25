package com.blockchain.logging

interface RemoteLogger {
    fun init(context: Any)
    fun logUserId(userId: String)
    fun logEvent(message: String) // Log something for crash debugging context
    fun logState(name: String, data: String) // Log a key/value property
    fun logException(throwable: Throwable, logMessage: String = "") // Log non-fatal exception catches
    fun logAndRethrowException(throwable: Throwable, logMessage: String = "") // Log non-fatal exception catches

    // Log various app state information. Extend this as required
    fun onlineState(isOnline: Boolean)
    fun userLanguageLocale(locale: String)
    fun logView(viewName: String)

    val isDebugBuild: Boolean
}
