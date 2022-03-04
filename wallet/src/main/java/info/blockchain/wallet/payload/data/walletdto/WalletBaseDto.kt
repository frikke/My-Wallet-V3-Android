package info.blockchain.wallet.payload.data.walletdto

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
class WalletBaseDto(
    // payload could be string in V1
    // V2 and up is WalletWrapper
    @field:JsonProperty("payload")
    var payload: String? = null,

    // V3
    @field:JsonProperty("guid")
    var guid: String? = null,

    @field:JsonProperty("extra_seed")
    var extraSeed: String? = null,

    @field:JsonProperty("payload_checksum")
    var payloadChecksum: String? = null,

    @field:JsonProperty("war_checksum")
    var warChecksum: String? = null,

    @field:JsonProperty("language")
    var language: String? = null,

    @field:JsonProperty("storage_token")
    var storageToken: String? = null,

    @field:JsonProperty("sync_pubkeys")
    var syncPubkeys: Boolean = false
)
