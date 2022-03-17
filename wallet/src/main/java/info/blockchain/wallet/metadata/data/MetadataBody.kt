package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
class MetadataBody(
    var version: Int = 0,
    var payload: String = "",
    var signature: String = "",
    @field:JsonProperty("prev_magic_hash")
    @SerialName("prev_magic_hash")
    var prevMagicHash: String? = null,
    @field:JsonProperty("type_id")
    @SerialName("type_id")
    var typeId: Int = 0
) {
    @JsonIgnore @Throws(JsonProcessingException::class)
    fun toJson(): String? {
        return ObjectMapper().writeValueAsString(this)
    }
}
