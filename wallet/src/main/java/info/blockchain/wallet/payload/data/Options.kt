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
class Options(
    @field:JsonProperty("pbkdf2_iterations")
    var pbkdf2Iterations: Int = 0,

    @field:JsonProperty("fee_per_kb")
    var feePerKb: Long = 0,

    @field:JsonProperty("html5_notifications")
    var isHtml5Notifications: Boolean = false,

    @field:JsonProperty("logout_time")
    var logoutTime: Long = 0
) {

    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
    companion object {
        private const val DEFAULT_FEE_PER_KB = 10000L
        private const val DEFAULT_LOGOUT_TIME = 600000L
        private const val DEFAULT_HTML5_NOTIFICATIONS = false

        @JvmStatic
        @Throws(IOException::class) fun fromJson(json: String): Options {
            return ObjectMapper().readValue(json, Options::class.java)
        }

        val defaultOptions: Options
            get() {
                val defaultOptions = Options()
                defaultOptions.pbkdf2Iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2
                defaultOptions.isHtml5Notifications = DEFAULT_HTML5_NOTIFICATIONS
                defaultOptions.logoutTime = DEFAULT_LOGOUT_TIME
                defaultOptions.feePerKb = DEFAULT_FEE_PER_KB
                return defaultOptions
            }
    }
}
