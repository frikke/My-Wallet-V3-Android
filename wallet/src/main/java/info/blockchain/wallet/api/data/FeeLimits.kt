package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class FeeLimits(
    @JsonProperty("min")
    @SerialName("min")
    var min: Long = 0,

    @JsonProperty("max")
    @SerialName("max")
    var max: Long = 0
)
