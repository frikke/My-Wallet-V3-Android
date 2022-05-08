package piuk.blockchain.androidcore.data.auth.metadata

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import piuk.blockchain.androidcore.utils.extensions.isValidGuid

@Serializable
data class WalletCredentialsMetadata(
    @SerialName("guid")
    val guid: String,

    @SerialName("password")
    val password: String,

    @SerialName("sharedKey")
    val sharedKey: String
) : JsonSerializable {

    fun isValid() = guid.isValidGuid() && password.isNotEmpty() && sharedKey.isNotEmpty()

    companion object {
        const val WALLET_CREDENTIALS_METADATA_NODE = 12
    }
}
