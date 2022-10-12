package com.blockchain.sunriver.datamanager

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class XlmAccount(
    /**
     * this the account ID/address that xlm uses
     */
    @SerialName("publicKey")
    val publicKey: String,
    /**
     * This the pubkey derived from app master key. Used in unified balances api
     */
    @SerialName("pubKey")
    val pubKey: String? = null,
    @SerialName("label")
    val label: String?,
    @SerialName("archived")
    private val _archived: Boolean?
) : JsonSerializable {
    val archived: Boolean
        get() = _archived ?: false
}
