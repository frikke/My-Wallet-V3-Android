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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
class EthereumWalletDto {
    @field:JsonProperty("ethereum")
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
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
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
        fun fromJson(json: String): EthereumWalletDto {
            val mapper = ObjectMapper()
            mapper.setVisibility(
                mapper.serializationConfig.defaultVisibilityChecker
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
            )
            return mapper.readValue(json, EthereumWalletDto::class.java)
        }
    }
}
