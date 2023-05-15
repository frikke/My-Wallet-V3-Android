package info.blockchain.wallet.coin

import com.blockchain.extensions.replace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * <p> Generic coin data that can be stored in blockchain.info KV store. </p>
 */
@Serializable
data class GenericMetadataWallet(
    @SerialName("default_account_idx")
    private val _defaultAcccountIdx: Int? = null,

    @SerialName("has_seen")
    private val _hasSeen: Boolean? = null,

    @SerialName("accounts")
    val accounts: List<GenericMetadataAccount>
) {
    val defaultAcccountIdx: Int
        get() = _defaultAcccountIdx ?: 0

    val hasSeen: Boolean
        get() = _hasSeen ?: false

    fun addAccount(account: GenericMetadataAccount): GenericMetadataWallet =
        this.copy(
            accounts = accounts.plus(account)
        )

    fun updateXpubForAccountIndex(index: Int, xPub: String): GenericMetadataWallet =
        this.copy(
            accounts = accounts.replace(
                old = accounts[index],
                new = accounts[index].updateXpub(xPub)
            )
        )

    fun toJson(): String {
        return jsonBuilder.encodeToString(returnSafeClone())
    }

    /**
     * Returns a deep clone of the current object, but strips out any xPubs from the [ ] objects, as we're not currently storing them in metadata but may be
     * serialising them in-app.
     *
     * @return A [GenericMetadataWallet] with xPubs removed
     */
    private fun returnSafeClone(): GenericMetadataWallet {
        return this.copy(
            accounts = accounts.map { it.removeXpub() }
        )
    }

    fun updateAccount(oldAccount: GenericMetadataAccount, newAccount: GenericMetadataAccount): GenericMetadataWallet {
        return copy(
            accounts = accounts.replace(oldAccount, newAccount)
        )
    }

    fun updatedAccounts(updatedAccounts: Map<GenericMetadataAccount, String>): GenericMetadataWallet {
        val updatedAccountList = accounts.map { account ->
            updatedAccounts[account]?.let { label ->
                account.updateLabel(label)
            } ?: account
        }
        return copy(accounts = updatedAccountList)
    }

    fun updateDefaultIndex(newIndex: Int): GenericMetadataWallet {
        return copy(
            _defaultAcccountIdx = newIndex
        )
    }

    companion object {
        private val jsonBuilder: Json = Json {
            ignoreUnknownKeys = true
        }

        fun fromJson(json: String): GenericMetadataWallet {
            return jsonBuilder.decodeFromString(serializer(), json)
        }
    }
}
