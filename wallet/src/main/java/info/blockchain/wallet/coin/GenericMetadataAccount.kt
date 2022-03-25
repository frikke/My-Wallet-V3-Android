package info.blockchain.wallet.coin

import com.blockchain.serialization.JsonSerializableAccount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs

/**
 * <p>
 *     Generic coin account data that can be stored in blockchain.info KV store.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
class GenericMetadataAccount(
    @JsonProperty("label")
    override var label: String = "",

    @field:JsonProperty("archived")
    var isArchived: Boolean = false,

    @JsonProperty("xpub")
    private var xpub: String? = null
) : JsonSerializableAccount {

    constructor(label: String, archived: Boolean) : this() {
        this.label = label
        isArchived = archived
    }

    fun setXpub(xpub: String?) {
        this.xpub = xpub
    }

    fun xpubs(): XPubs {
        return XPubs(XPub(xpub!!, XPub.Format.LEGACY))
    }
}
