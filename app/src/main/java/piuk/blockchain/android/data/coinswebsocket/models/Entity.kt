package piuk.blockchain.android.data.coinswebsocket.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Entity {
    @SerialName("height")
    Height,
    @SerialName("header")
    Header,
    @SerialName("confirmed_transaction")
    ConfirmedTransaction,
    @SerialName("pending_transaction")
    PendingTransaction,
    @SerialName("account")
    Account,
    @SerialName("xpub")
    Xpub,
    @SerialName("token_transfer")
    TokenTransfer,
    @SerialName("token_account_delta")
    TokenAccountDelta,
    @SerialName("token_account")
    TokenAccount,
    @SerialName("wallet")
    Wallet,
    @SerialName("none")
    NONE
}
