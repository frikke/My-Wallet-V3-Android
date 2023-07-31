package com.blockchain.domain.auth

import java.io.Serializable

data class SecureChannelBrowserMessage(
    val type: String,
    val channelId: String,
    val timestamp: Long
) : Serializable
