package com.blockchain.preferences

interface RemoteConfigPrefs {
    val isRemoteConfigStale: Boolean
    fun updateRemoteConfigStaleStatus(isStale: Boolean)

    val brokerageErrorsEnabled: Boolean
    fun updateBrokerageErrorStatus(enabled: Boolean)

    val brokerageErrorsCode: String
    fun updateBrokerageErrorCode(code: String)
}
