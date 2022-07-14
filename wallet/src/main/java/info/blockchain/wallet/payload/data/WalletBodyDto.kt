package info.blockchain.wallet.payload.data

import com.blockchain.extensions.replace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * HD Wallet paylot
 */
@Serializable
data class WalletBodyDto(
    @SerialName("accounts")
    val accounts: List<Account>,
    @SerialName("seed_hex")
    val seedHex: String,
    @SerialName("passphrase")
    val passphrase: String,
    /**
     * Missing in old payloads :-
     */
    @SerialName("mnemonic_verified")
    private val _mnemonicVerified: Boolean? = null,
    /**
     * We fallback to -1 if missing
     */
    @SerialName("default_account_idx")
    val defaultAccountIdx: Int? = MISSING_DEFAULT_INDEX_VALUE,
) {
    fun withUpdatedAccounts(accounts: List<Account>): WalletBodyDto =
        this.copy(accounts = accounts)

    val mnemonicVerified: Boolean
        get() = _mnemonicVerified ?: false

    fun updateAccountLabel(account: Account, label: String): WalletBodyDto {
        val mAccount = accounts.first { it == account }
        return this.copy(
            accounts = accounts.replace(mAccount, mAccount.updateLabel(label))
        )
    }

    fun updateAccountArchivedState(account: Account, archived: Boolean): WalletBodyDto {
        val mAccount = accounts.first { it == account }
        return this.copy(
            accounts = accounts.replace(mAccount, mAccount.updateArchivedState(archived))
        )
    }
}

const val MISSING_DEFAULT_INDEX_VALUE = -1
