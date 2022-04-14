package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AndroidUpgrade(
    @SerialName("latestStoreVersion")
    val latestStoreVersion: String = "",
    @SerialName("updateType")
    val updateType: String = ""
)

enum class UpdateType {
    NONE, RECOMMENDED, FORCE
}
