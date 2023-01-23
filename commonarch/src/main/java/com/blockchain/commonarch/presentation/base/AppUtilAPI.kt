package com.blockchain.commonarch.presentation.base

interface AppUtilAPI {
    fun logout(isIntercomEnabled: Boolean = false)
    fun restartApp()
    var activityIndicator: ActivityIndicator?
}
