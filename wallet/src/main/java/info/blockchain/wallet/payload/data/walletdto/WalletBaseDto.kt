package info.blockchain.wallet.payload.data.walletdto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class WalletBaseDto(
    // payload could be string in V1
    // V2 and up is WalletWrapper
    @SerialName("payload")
    var payload: String? = null,

    // V3
    @SerialName("guid")
    var guid: String? = null,

    @SerialName("extra_seed")
    var extraSeed: String? = null,

    @SerialName("payload_checksum")
    var payloadChecksum: String? = null,

    @SerialName("war_checksum")
    var warChecksum: String? = null,

    @SerialName("language")
    var language: String? = null,

    @SerialName("storage_token")
    var storageToken: String? = null,

    @SerialName("sync_pubkeys")
    var syncPubkeys: Boolean = false
) {

    fun toJson() = Json.encodeToString(this)

    companion object {
        @JvmStatic
        fun fromJson(json: String): WalletBaseDto {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
            }
            return jsonBuilder.decodeFromString(json)
        }
    }
}
