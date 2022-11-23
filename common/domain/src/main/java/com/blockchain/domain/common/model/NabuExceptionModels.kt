package com.blockchain.domain.common.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerSideUxErrorInfo(
    val id: String?,
    val title: String,
    val description: String,
    val iconUrl: String,
    val statusUrl: String,
    val actions: List<ServerErrorAction>,
    val categories: List<String>
) : java.io.Serializable

@Serializable
data class ServerErrorAction(
    val title: String,
    val deeplinkPath: String
) : java.io.Serializable
