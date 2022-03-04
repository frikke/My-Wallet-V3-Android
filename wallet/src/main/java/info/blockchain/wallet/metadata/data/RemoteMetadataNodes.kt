package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class RemoteMetadataNodes(
    /**
     * Private bytes as HEX
     */
    var metadata: String = "",
    var mdid: String = "",
    // Add any future metadata node derivations here
) {
    @JsonIgnore
    fun isAllNodesAvailable(): Boolean = metadata.isNotEmpty() && mdid.isNotEmpty()

    @JsonIgnore @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    companion object {
        @JsonIgnore @Throws(IOException::class)
        fun fromJson(json: String): RemoteMetadataNodes {
            return ObjectMapper().readValue(json, RemoteMetadataNodes::class.java)
        }
    }
}
