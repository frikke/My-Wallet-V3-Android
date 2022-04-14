package info.blockchain.wallet.ethereum

import java.util.ArrayList
import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class EthereumWalletDto {
    @SerialName("ethereum")
    var walletData: EthereumWalletData? = null

    constructor(
        accounts: ArrayList<EthereumAccount>
    ) {
        walletData = EthereumWalletData().apply {
            hasSeen = false
            defaultAccountIdx = 0
            txNotes = HashMap()
            this.accounts = accounts
        }
    }

    fun toJson(): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        return jsonBuilder.encodeToString(this)
    }

    /**
     * @return Single Ethereum account
     */
    val account: EthereumAccount?
        get() = if (walletData!!.accounts!!.isEmpty()) {
            null
        } else walletData!!.accounts!![ACCOUNT_INDEX]

    val txNotes: HashMap<String, String>?
        get() = walletData!!.txNotes

    companion object {
        const val METADATA_TYPE_EXTERNAL = 5
        private const val ACCOUNT_INDEX = 0

        @JvmStatic
        fun fromJson(json: String): EthereumWalletDto {
            val jsonBuilder = Json { ignoreUnknownKeys = true }
            return jsonBuilder.decodeFromString(serializer(), json)
        }
    }
}
