package info.blockchain.wallet.ethereum

import com.blockchain.extensions.replace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EthereumWalletData(
    @SerialName("has_seen")
    private val _hasSeen: Boolean? = null,
    @SerialName("default_account_idx")
    private val _defaultAccountIdx: Int? = null,
    @SerialName("accounts")
    private val _accounts: List<EthAccountDto>? = null,
    @SerialName("erc20")
    private val _erc20Tokens: Map<String, Erc20TokenData>? = null,
    @SerialName("legacy_account")
    val legacyAccount: EthAccountDto? = null,
    @SerialName("tx_notes")
    private val _txNotes: Map<String, String>? = null,
    @SerialName("last_tx")
    val lastTx: String? = null,
    @SerialName("last_tx_timestamp")
    val _lastTxTimestamp: Long? = null
) {
    val accounts: List<EthAccountDto>
        get() = _accounts ?: emptyList()
    val hasSeen: Boolean
        get() = _hasSeen ?: false

    val erc20Tokens: Map<String, Erc20TokenData>
        get() = _erc20Tokens ?: emptyMap()

    val txNotes: Map<String, String>
        get() = _txNotes ?: emptyMap()

    fun updateTxNotes(hash: String, note: String): EthereumWalletData =
        copy(
            _txNotes = txNotes.plus(hash to note)
        )

    fun withRenamedAccount(label: String): EthereumWalletData =
        copy(
            _accounts = accounts.replace(old = accounts[0], new = accounts[0].rename(label))
        )

    fun withUpdatedNoteForErc20(hash: String, note: String, erc20: Erc20TokenData): EthereumWalletData {
        val newErc20 = erc20Tokens.values.first { it == erc20 }.putTxNote(hash, note)
        return copy(
            _erc20Tokens = erc20Tokens.filterValues { it != erc20 }
                .plus(
                    erc20Tokens.filterValues { it == erc20 }.keys.first() to newErc20
                )
        )
    }

    fun withCheckedSummedAccount(): EthereumWalletData {
        return copy(
            _accounts = accounts.replace(old = accounts[0], new = accounts[0].withChecksummedAddress())
        )
    }

    val defaultAccountIdx: Int
        get() = _defaultAccountIdx ?: 0

    val lastTxTimestamp: Long
        get() = _lastTxTimestamp ?: 0
}
