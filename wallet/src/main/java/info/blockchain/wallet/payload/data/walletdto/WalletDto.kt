package info.blockchain.wallet.payload.data.walletdto

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

@Serializable
internal class WalletDto constructor(
    @SerialName("guid")
    var guid: String? = null,

    @SerialName("sharedKey")
    var sharedKey: String? = null,

    @SerialName("double_encryption")
    val isDoubleEncryption: Boolean = false,

    @SerialName("dpasswordhash")
    val dpasswordhash: String? = null,

    @SerialName("metadataHDNode")
    val metadataHDNode: String? = null,

    @SerialName("tx_notes")
    var txNotes: MutableMap<String, String>? = null,

    @SerialName("tx_tags")
    val txTags: Map<String, List<Int>>? = null,

    @SerialName("tag_names")
    val tagNames: List<Map<Int, String>>? = null,

    @SerialName("options")
    var options: Options? = null,

    @SerialName("address_book")
    var addressBook: List<AddressBook>? = null,

    @SerialName("wallet_options")
    val walletOptions: Options? = null,

    @SerialName("hd_wallets")
    var walletBodies: List<WalletBody>? = null,

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
