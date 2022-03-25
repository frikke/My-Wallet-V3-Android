package piuk.blockchain.androidcore.data.auth.metadata

import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import piuk.blockchain.androidcore.utils.extensions.isValidGuid

@Serializable
data class WalletCredentialsMetadata(
    @SerialName("guid")
    @field:Json(name = "guid")
    val guid: String,

    @SerialName("password")
    @field:Json(name = "password")
    val password: String,

    @SerialName("sharedKey")
    @field:Json(name = "sharedKey")
    val sharedKey: String
) : JsonSerializable {

    fun isValid() = guid.isValidGuid() && password.isNotEmpty() && sharedKey.isNotEmpty()

    companion object {
        const val WALLET_CREDENTIALS_METADATA_NODE = 12
    }
}
