package info.blockchain.wallet.payload.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
class AddressBook(
    @JsonProperty("label")
    var label: String? = null,

    @field:JsonProperty("addr")
    var address: String? = null
) {
    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    companion object {

        @JvmStatic
        @Throws(IOException::class)
        fun fromJson(json: String): AddressBook {
            return ObjectMapper().readValue(json, AddressBook::class.java)
        }
    }
}
