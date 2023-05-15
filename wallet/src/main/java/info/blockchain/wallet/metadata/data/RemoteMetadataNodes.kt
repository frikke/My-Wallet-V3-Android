package info.blockchain.wallet.metadata.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class RemoteMetadataNodes(
    /**
     * Private bytes as HEX
     */
    @SerialName("metadata")
    var metadata: String = "",
    @SerialName("mdid")
    var mdid: String = ""
    // Add any future metadata node derivations here
) {
    fun isAllNodesAvailable(): Boolean = metadata.isNotEmpty() && mdid.isNotEmpty()

    fun toJson(): String {
        return jsonBuilder.encodeToString(this)
    }

    companion object {
        private val jsonBuilder: Json = Json {
            ignoreUnknownKeys = true
        }

        fun fromJson(json: String): RemoteMetadataNodes {
            return jsonBuilder.decodeFromString(serializer(), json)
        }
    }
}
