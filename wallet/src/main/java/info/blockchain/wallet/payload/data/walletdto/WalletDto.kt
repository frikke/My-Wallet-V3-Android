package info.blockchain.wallet.payload.data.walletdto

import com.blockchain.extensions.replace
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Options
import info.blockchain.wallet.payload.data.Options.Companion.defaultOptions
import info.blockchain.wallet.payload.data.WalletBodyDto
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class WalletDto(
    @SerialName("guid")
    val guid: String,

    @SerialName("sharedKey")
    val sharedKey: String,

    @SerialName("double_encryption")
    private val _isDoubleEncryption: Boolean? = null,

    @SerialName("dpasswordhash")
    private val _dpasswordhash: String? = null,

    @SerialName("metadataHDNode")
    val metadataHDNode: String? = null,

    @SerialName("tx_notes")
    val txNotes: Map<String, String>? = null,

    @SerialName("tag_names")
    val tagNames: List<Map<Int, String>>? = null,

    @SerialName("options")
    private val _options: Options? = null,

    @SerialName("wallet_options")
    private val walletOptions: Options? = null,
    /**
     * This can be null in case of <V3 wallets
     */
    @SerialName("hd_wallets")
    val walletBodies: List<WalletBodyDto>? = null,

    @SerialName("keys")
    private val _imported: List<ImportedAddress>? = null
) {
    val options: Options
        get() = _options ?: _options.fixPbkdf2Iterations()

    val doubledEncrypted: Boolean
        get() = _isDoubleEncryption ?: false

    val dpasswordhash: String
        get() = _dpasswordhash ?: ""

    val imported: List<ImportedAddress>
        get() = _imported ?: emptyList()

    constructor(walletBodies: List<WalletBodyDto>) : this(
        guid = UUID.randomUUID().toString(),
        sharedKey = UUID.randomUUID().toString(),
        txNotes = hashMapOf(),
        _imported = listOf(),
        _dpasswordhash = null,
        _isDoubleEncryption = false,
        metadataHDNode = null,
        tagNames = emptyList(),
        _options = defaultOptions,
        walletOptions = null,
        walletBodies = walletBodies
    )

    fun addWalletBody(walletBody: WalletBodyDto): WalletDto =
        this.copy(
            walletBodies = walletBodies?.plus(walletBody) ?: listOf(walletBody)
        )

    fun replaceWalletBodies(walletBodies: List<WalletBodyDto>): WalletDto =
        this.copy(
            walletBodies = walletBodies
        )

    fun replaceImportedAddress(oldAddress: ImportedAddress, newAddress: ImportedAddress): WalletDto =
        this.copy(
            _imported = _imported?.replace(oldAddress, newAddress) ?: listOf(newAddress)
        )

    fun addImportedAddress(address: ImportedAddress): WalletDto =
        this.copy(
            _imported = _imported?.plus(address) ?: listOf(address)
        )

    fun toJson(): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        return jsonBuilder.encodeToString(this)
    }

    fun replaceWalletBody(oldWalletBodyDto: WalletBodyDto, newWalletBodyDto: WalletBodyDto): WalletDto =
        this.copy(
            walletBodies = walletBodies!!.replace(oldWalletBodyDto, newWalletBodyDto)
        )

    /**
     * In case wallet was encrypted with iterations other than what is specified in options, we
     * will ensure next encryption and options get updated accordingly.
     *
     * @return
     */
    private fun Options?.fixPbkdf2Iterations(): Options {
        // Use default initially
        //  val iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2

        // Old wallets may contain 'wallet_options' key - we'll use this now
        if (
            walletOptions?.pbkdf2Iterations != null && walletOptions.pbkdf2Iterations > 0 &&
            this?.pbkdf2Iterations == null
        ) {
            val options = _options ?: defaultOptions
            return options.copy(
                pbkdf2Iterations = walletOptions.pbkdf2Iterations
            )
        }

        // 'options' key override wallet_options key - we'll use this now
        if (this?.pbkdf2Iterations != null && this.pbkdf2Iterations > 0) {
            return this
        }
        // If wallet doesn't contain 'option' - use default
        if (this?.pbkdf2Iterations == null) {
            return defaultOptions
        }
        return this
    }

    fun updateArchivedStateOfImportedAddr(address: ImportedAddress, isArchived: Boolean): WalletDto =
        this.copy(
            _imported = _imported?.replace(address, address.updateArchivedState(isArchived))
        )

    fun withPbkdf2Iterations(iterations: Int): WalletDto =
        this.copy(_options = _options?.copy(pbkdf2Iterations = iterations))

    fun withUpdatedNotes(transactionHash: String, notes: String): WalletDto {
        return this.copy(
            txNotes = txNotes?.plus(transactionHash to notes) ?: mapOf(transactionHash to notes)
        )
    }

    fun updateImportedAddressLabel(address: ImportedAddress, label: String): WalletDto {
        val updatedAddress = address.copy(labelField = label)
        return this.copy(_imported = imported.replace(old = address, new = updatedAddress))
    }

    companion object {
        @JvmStatic
        fun fromJson(json: String): WalletDto {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
            }
            return jsonBuilder.decodeFromString(json)
        }
    }
}
