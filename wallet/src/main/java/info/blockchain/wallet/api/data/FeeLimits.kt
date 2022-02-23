package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeLimits(
    @JsonProperty("min")
    var min: Long = 0,

    @JsonProperty("max")
    var max: Long = 0
)
