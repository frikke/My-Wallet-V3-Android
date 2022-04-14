package info.blockchain.wallet.metadata.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetadataResponse(
    val version: Int = 0,
    val payload: String = "",
    val signature: String = "",
    @SerialName("prev_magic_hash")
    val prevMagicHash: String? = null,
    @SerialName("type_id")
    val typeId: Int = 0,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("updated_at")
    val updatedAt: Long = 0,
    val address: String = ""
)
