package com.blockchain.commonarch.presentation.base

interface AppUtilAPI {
    fun logout(isIntercomEnabled: Boolean = false)
    fun restartApp(redirectLandingToLogin: Boolean = false)
    var activityIndicator: ActivityIndicator?
}
