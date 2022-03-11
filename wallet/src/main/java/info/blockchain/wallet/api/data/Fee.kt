package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@Serializable
data class Fee(
    @JsonProperty("fee")
    @SerialName("fee")
    var fee: Double = 0.0,

    @JsonProperty("surge")
    @SerialName("surge")
    var surge: Boolean = false,

    @JsonProperty("ok")
    @SerialName("ok")
    var ok: Boolean = false
)
