package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Auth(
    var nonce: String,
    var token: String
) {
    @JsonIgnore @Throws(JsonProcessingException::class)
    fun toJson(): String? {
        return ObjectMapper().writeValueAsString(this)
    }
}
