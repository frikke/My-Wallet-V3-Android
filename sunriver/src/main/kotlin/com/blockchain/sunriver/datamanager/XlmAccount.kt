package com.blockchain.sunriver.datamanager

import com.blockchain.serialization.JsonSerializableAccount
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
    private val _label: String? = null,
    @SerialName("archived")
    private val _archived: Boolean?
) : JsonSerializableAccount {
    override val isArchived: Boolean
        get() = _archived ?: false

    override val label: String
        get() = _label.orEmpty()

    override fun updateArchivedState(isArchived: Boolean): JsonSerializableAccount {
        return copy(_archived = isArchived)
    }
}
