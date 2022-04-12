package info.blockchain.wallet.metadata.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MetadataBody(
    val version: Int = 0,
    val payload: String = "",
    val signature: String = "",
    @SerialName("prev_magic_hash")
    val prevMagicHash: String? = null,
    @SerialName("type_id")
    val typeId: Int = 0
)
