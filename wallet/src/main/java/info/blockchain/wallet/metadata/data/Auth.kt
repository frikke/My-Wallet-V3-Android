package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
class Auth(
    @SerialName("nonce")
    var nonce: String,
    @SerialName("token")
    var token: String
) {
    @JsonIgnore @Throws(JsonProcessingException::class)
    fun toJson(): String? {
        return ObjectMapper().writeValueAsString(this)
    }
}
