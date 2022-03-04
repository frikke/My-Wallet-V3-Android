package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
data class MetadataResponse(
    val version: Int = 0,
    val payload: String = "",
    val signature: String = "",
    @field:JsonProperty("prev_magic_hash")
    val prevMagicHash: String? = null,
    @field:JsonProperty("type_id")
    val typeId: Int = 0,
    @field:JsonProperty("created_at")
    val createdAt: Long = 0,
    @field:JsonProperty("updated_at")
    val updatedAt: Long = 0,
    val address: String = ""
) {
    @JsonIgnore
    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}
