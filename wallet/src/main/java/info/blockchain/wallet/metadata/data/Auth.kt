package info.blockchain.wallet.metadata.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Auth(
    @SerialName("nonce")
    val nonce: String,
    @SerialName("token")
    val token: String
)
