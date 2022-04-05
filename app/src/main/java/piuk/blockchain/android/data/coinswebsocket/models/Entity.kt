package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Entity {
    @SerializedName("height")
    @SerialName("height")
    Height,
    @SerializedName("header")
    @SerialName("header")
    Header,
    @SerializedName("confirmed_transaction")
    @SerialName("confirmed_transaction")
    ConfirmedTransaction,
    @SerializedName("pending_transaction")
    @SerialName("pending_transaction")
    PendingTransaction,
    @SerializedName("account")
    @SerialName("account")
    Account,
    @SerializedName("xpub")
    @SerialName("xpub")
    Xpub,
    @SerializedName("token_transfer")
    @SerialName("token_transfer")
    TokenTransfer,
    @SerializedName("token_account_delta")
    @SerialName("token_account_delta")
    TokenAccountDelta,
    @SerializedName("token_account")
    @SerialName("token_account")
    TokenAccount,
    @SerializedName("wallet")
    @SerialName("wallet")
    Wallet,
    @SerialName("none")
    NONE
}
