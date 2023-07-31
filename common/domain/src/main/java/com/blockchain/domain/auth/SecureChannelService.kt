package com.blockchain.domain.auth

import kotlinx.coroutines.flow.Flow

interface SecureChannelService {
    fun sendErrorMessage(channelId: String, pubKeyHash: String)
    fun sendHandshake(json: String)
    fun sendLoginMessage(channelId: String, pubKeyHash: String)
    fun decryptMessage(pubKeyHash: String, messageEncrypted: String): SecureChannelBrowserMessage?
    suspend fun secureChannelLogin(payload: Map<String, String?>)
    val secureLoginAttempted: Flow<SecureChannelLoginData>
}

data class SecureChannelLoginData(
    val pubKeyHash: String,
    val message: SecureChannelBrowserMessage,
    val originIp: String,
    val originLocation: String,
    val originBrowser: String
)
