package info.blockchain.wallet.ethereum

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class EthereumWalletDto(
    @SerialName("ethereum")
    val walletData: EthereumWalletData
) : JsonSerializable {
    constructor(
        accounts: List<EthAccountDto>
    ) : this(
        EthereumWalletData(
            _hasSeen = false,
            _defaultAccountIdx = 0,
            _txNotes = emptyMap(),
            _accounts = accounts
        )
    )

    fun toJson(): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        return jsonBuilder.encodeToString(this)
    }

    fun updateTxNotes(hash: String, note: String): EthereumWalletDto {
        return EthereumWalletDto(
            walletData = walletData.updateTxNotes(hash, note)
        )
    }

    fun renameAccount(label: String): EthereumWalletDto =
        EthereumWalletDto(
            walletData = walletData.withRenamedAccount(label)
        )

    fun updateTxNoteForErc20(hash: String, note: String, erc20: Erc20TokenData): EthereumWalletDto {
        return EthereumWalletDto(
            walletData = walletData.withUpdatedNoteForErc20(hash, note, erc20)
        )
    }

    fun withCheckedSummedAccount(): EthereumWalletDto =
        EthereumWalletDto(
            walletData = walletData.withCheckedSummedAccount()
        )

    /**
     * @return Single Ethereum account
     */
    val account: EthereumAccount?
        get() = if (walletData.accounts.isEmpty()) {
            null
        } else {
            EthereumAccount(walletData.accounts[ACCOUNT_INDEX])
        }

    val txNotes: Map<String, String>
        get() = walletData.txNotes

    companion object {
        private const val ACCOUNT_INDEX = 0

        @JvmStatic
        fun fromJson(json: String): EthereumWalletDto {
            val jsonBuilder = Json { ignoreUnknownKeys = true }
            return jsonBuilder.decodeFromString(serializer(), json)
        }
    }
}
