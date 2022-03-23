package com.blockchain.sunriver.datamanager

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class XlmAccount(
    @SerialName("publicKey")
    val publicKey: String,

    @SerialName("label")
    val label: String?,

    @SerialName("archived")
    val archived: Boolean? = false
) : JsonSerializable
