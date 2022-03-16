package info.blockchain.wallet.payload.data.walletdto

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import info.blockchain.wallet.payload.data.AddressBook
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Options
import info.blockchain.wallet.payload.data.Options.Companion.defaultOptions
import info.blockchain.wallet.payload.data.WalletBody
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@Serializable
internal class WalletDto constructor(
    @field:JsonProperty("guid")
    @SerialName("guid")
    var guid: String? = null,

    @field:JsonProperty("sharedKey")
    @SerialName("sharedKey")
    var sharedKey: String? = null,

    @field:JsonProperty("double_encryption")
    @SerialName("double_encryption")
    val isDoubleEncryption: Boolean = false,

    @field:JsonProperty("dpasswordhash")
    @SerialName("dpasswordhash")
    val dpasswordhash: String? = null,

    @field:JsonProperty("metadataHDNode")
    @SerialName("metadataHDNode")
    val metadataHDNode: String? = null,

    @field:JsonProperty("tx_notes")
    @SerialName("tx_notes")
    var txNotes: MutableMap<String, String>? = null,

    @field:JsonProperty("tx_tags")
    @SerialName("tx_tags")
    val txTags: Map<String, List<Int>>? = null,

    @field:JsonProperty("tag_names")
    @SerialName("tag_names")
    val tagNames: List<Map<Int, String>>? = null,

    @field:JsonProperty("options")
    @SerialName("options")
    var options: Options? = null,

    @field:JsonProperty("address_book")
    @SerialName("address_book")
    var addressBook: List<AddressBook>? = null,

    @field:JsonProperty("wallet_options")
    @SerialName("wallet_options")
    val walletOptions: Options? = null,

    @field:JsonProperty("hd_wallets")
    @SerialName("hd_wallets")
    var walletBodies: List<WalletBody>? = null,

    @field:JsonProperty("keys")
    @SerialName("keys")
    var imported: MutableList<ImportedAddress>? = null
) {
    constructor() : this(
        guid = UUID.randomUUID().toString(),
        sharedKey = UUID.randomUUID().toString(),
        txNotes = hashMapOf(),
        imported = mutableListOf(),
        options = defaultOptions,
        walletBodies = emptyList()
    )

    constructor(walletBodies: List<WalletBody>) : this(
        guid = UUID.randomUUID().toString(),
        sharedKey = UUID.randomUUID().toString(),
        txNotes = hashMapOf(),
        imported = mutableListOf(),
        options = defaultOptions,
        walletBodies = walletBodies
    )

    fun toJson(module: SerializersModule): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
            serializersModule = module
            encodeDefaults = true
        }
        return jsonBuilder.encodeToString(this)
    }

    companion object {
        @JvmStatic
        fun fromJson(json: String, module: SerializersModule): WalletDto {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
                serializersModule = module
            }
            return jsonBuilder.decodeFromString(json)
        }
    }
}
