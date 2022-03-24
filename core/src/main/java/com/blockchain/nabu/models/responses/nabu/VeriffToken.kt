package com.blockchain.nabu.models.responses.nabu

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.Serializable

@Serializable
data class VeriffToken(
    val applicantId: String,
    val token: String,
    val data: Data
) : JsonSerializable

@Serializable
data class Data(
    val url: String
) : JsonSerializable
