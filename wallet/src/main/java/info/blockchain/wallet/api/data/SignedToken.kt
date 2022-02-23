package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SignedToken(
    @JsonProperty("success")
    var isSuccessful: Boolean = false,
    @JsonProperty("token")
    val token: String? = null,
    @JsonProperty("error")
    val error: String? = null
)
