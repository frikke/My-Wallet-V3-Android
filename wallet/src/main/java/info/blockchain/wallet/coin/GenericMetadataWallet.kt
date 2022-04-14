package info.blockchain.wallet.coin

import java.util.ArrayList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * <p> Generic coin data that can be stored in blockchain.info KV store. </p>
 */
@Serializable
class GenericMetadataWallet(
    @SerialName("default_account_idx")
    var defaultAcccountIdx: Int = 0,

    @SerialName("has_seen")
    var isHasSeen: Boolean = false,

    @SerialName("accounts")
    var accounts: MutableList<GenericMetadataAccount> = mutableListOf()
) {

    fun addAccount(account: GenericMetadataAccount) {
        accounts.add(account)
    }

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
        val safeAccounts: MutableList<GenericMetadataAccount> = ArrayList()
        for (account in accounts) {
            val safeClone = GenericMetadataAccount(account.label, account.isArchived)
            safeAccounts.add(safeClone)
        }
        return GenericMetadataWallet(defaultAcccountIdx, isHasSeen, safeAccounts)
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
