package com.blockchain.sunriver.datamanager

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class XlmMetaData(

    @SerialName("default_account_idx")
    val defaultAccountIndex: Int = 0,

    @SerialName("accounts")
    val accounts: List<XlmAccount>?,

    @SerialName("tx_notes")
    val transactionNotes: Map<String, String>?
) : JsonSerializable

internal fun XlmMetaData.default() = accounts!![defaultAccountIndex]
