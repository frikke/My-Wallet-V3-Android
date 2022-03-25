package info.blockchain.wallet.coin

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.util.ArrayList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * <p> Generic coin data that can be stored in blockchain.info KV store. </p>
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
@Serializable
class GenericMetadataWallet(
    @field:JsonProperty("default_account_idx")
    @SerialName("default_account_idx")
    var defaultAcccountIdx: Int = 0,

    @field:JsonProperty("has_seen")
    @SerialName("has_seen")
    var isHasSeen: Boolean = false,

    @JsonProperty("accounts")
    @SerialName("accounts")
    var accounts: MutableList<GenericMetadataAccount> = mutableListOf()
) {

    fun addAccount(account: GenericMetadataAccount) {
        accounts.add(account)
    }

    @Throws(JsonProcessingException::class)
    fun toJson(withKotlinX: Boolean): String {
        return if (withKotlinX) {
            jsonBuilder.encodeToString(returnSafeClone())
        } else {
            ObjectMapper().writeValueAsString(returnSafeClone())
        }
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

        @Throws(IOException::class)
        fun fromJson(json: String, withKotlinX: Boolean): GenericMetadataWallet {
            return if (withKotlinX) {
                jsonBuilder.decodeFromString(serializer(), json)
            } else {
                ObjectMapper().readValue(json, GenericMetadataWallet::class.java)
            }
        }
    }
}
