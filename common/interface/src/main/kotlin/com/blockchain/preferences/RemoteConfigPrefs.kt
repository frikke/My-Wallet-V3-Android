package com.blockchain.preferences

interface RemoteConfigPrefs {
    val isRemoteConfigStale: Boolean
    fun updateRemoteConfigStaleStatus(isStale: Boolean)
}
