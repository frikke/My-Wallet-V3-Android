package info.blockchain.wallet.payload.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import info.blockchain.wallet.bip44.HDAccount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
This class is used for iOS and Web only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE
)
@Serializable
data class AddressCache(
    @JsonProperty("receiveAccount")
    @SerialName("receiveAccount")
    var receiveAccount: String? = null,
    @JsonProperty("changeAccount")
    @SerialName("changeAccount")
    var changeAccount: String? = null
) {
    companion object {
        fun setCachedXPubs(account: HDAccount): AddressCache {
            return AddressCache(
                receiveAccount = account.receive.xpub,
                changeAccount = account.change.xpub
            )
        }
    }
}
