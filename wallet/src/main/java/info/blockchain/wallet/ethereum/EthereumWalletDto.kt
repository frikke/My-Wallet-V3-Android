package info.blockchain.wallet.ethereum

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
class EthereumWalletDto {
    @field:JsonProperty("ethereum")
    @SerialName("ethereum")
    var walletData: EthereumWalletData? = null

    constructor() {
        // default constructor for Jackson
    }

    constructor(
        accounts: ArrayList<EthereumAccount>
    ) {
        walletData = EthereumWalletData().apply {
            hasSeen = false
            defaultAccountIdx = 0
            txNotes = HashMap()
            this.accounts = accounts
        }
    }

    @Throws(JsonProcessingException::class)
    fun toJson(withKotlinX: Boolean): String {
        return if (withKotlinX) {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            return jsonBuilder.encodeToString(this)
        } else {
            ObjectMapper().writeValueAsString(this)
        }
    }

    /**
     * @return Single Ethereum account
     */
    val account: EthereumAccount?
        get() = if (walletData!!.accounts!!.isEmpty()) {
            null
        } else walletData!!.accounts!![ACCOUNT_INDEX]

    val txNotes: HashMap<String, String>?
        get() = walletData!!.txNotes

    companion object {
        const val METADATA_TYPE_EXTERNAL = 5
        private const val ACCOUNT_INDEX = 0

        @JvmStatic
        @Throws(IOException::class)
        fun fromJson(json: String, withKotlinX: Boolean): EthereumWalletDto {
            return if (withKotlinX) {
                val jsonBuilder = Json { ignoreUnknownKeys = true }
                jsonBuilder.decodeFromString(serializer(), json)
            } else {
                val mapper = ObjectMapper()
                mapper.setVisibility(
                    mapper.serializationConfig.defaultVisibilityChecker
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                )
                mapper.readValue(json, EthereumWalletDto::class.java)
            }
        }
    }
}
