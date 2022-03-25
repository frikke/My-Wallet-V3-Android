package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
class RemoteMetadataNodes(
    /**
     * Private bytes as HEX
     */
    @SerialName("metadata")
    var metadata: String = "",
    @SerialName("mdid")
    var mdid: String = "",
    // Add any future metadata node derivations here
) {
    @JsonIgnore
    fun isAllNodesAvailable(): Boolean = metadata.isNotEmpty() && mdid.isNotEmpty()

    @JsonIgnore @Throws(JsonProcessingException::class)
    fun toJson(withKotlinX: Boolean): String {
        return if (withKotlinX) {
            jsonBuilder.encodeToString(this)
        } else {
            ObjectMapper().writeValueAsString(this)
        }
    }

    companion object {
        private val jsonBuilder: Json = Json {
            ignoreUnknownKeys = true
        }

        @JsonIgnore @Throws(IOException::class)
        fun fromJson(json: String, withKotlinX: Boolean): RemoteMetadataNodes {
            return if (withKotlinX) {
                jsonBuilder.decodeFromString(serializer(), json)
            } else {
                ObjectMapper().readValue(json, RemoteMetadataNodes::class.java)
            }
        }
    }
}
