package com.blockchain.commonarch.presentation.base

interface AppUtilAPI {
    fun logout(isIntercomEnabled: Boolean = false)
    var activityIndicator: ActivityIndicator?
}
