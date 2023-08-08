package info.blockchain.wallet.payload.data.walletdto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WalletBaseDto(
    // payload could be string in V1
    // V2 and up is WalletWrapper
    @SerialName("payload")
    val payload: String,
    // V3
    @SerialName("guid")
    val guid: String? = null,

    @SerialName("extra_seed")
    val extraSeed: String? = null,

    @SerialName("payload_checksum")
    val payloadChecksum: String? = null,

    @SerialName("war_checksum")
    private val warChecksum: String? = null,

    @SerialName("language")
    private val language: String? = null,

    @SerialName("storage_token")
    private val storageToken: String? = null,

    @SerialName("sync_pubkeys")
    private val _syncPubkeys: Boolean? = null
) {

    fun toJson() = Json.encodeToString(this)

    fun withUpdatedPayloadCheckSum(newChecksum: String): WalletBaseDto =
        this.copy(payloadChecksum = newChecksum)

    fun withSyncedKeys(): WalletBaseDto {
        return this.copy(
            _syncPubkeys = true
        )
    }

    val syncPubkeys: Boolean
        get() = _syncPubkeys ?: false

    companion object {
        @JvmStatic
        fun fromJson(json: String): WalletBaseDto {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
            }
            return jsonBuilder.decodeFromString(json)
        }

        fun withDefaults() =
            WalletBaseDto(
                payload = "",
                guid = "",
                extraSeed = "",
                payloadChecksum = "",
                warChecksum = "",
                language = "",
                storageToken = "",
                _syncPubkeys = false
            )
    }
}
