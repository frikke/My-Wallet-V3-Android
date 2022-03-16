package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class Settings(

    @field:JsonProperty("btc_currency")
    @SerialName("btc_currency")
    private val btcCurrency: String = "",
    @field:JsonProperty("notifications_type")
    @SerialName("notifications_type")
    val notificationsType: List<Int> = emptyList(),
    @field:JsonProperty("language")
    @SerialName("language")
    private val language: String = "",
    @field:JsonProperty("notifications_on")
    @SerialName("notifications_on")
    private val notificationsOn: Int = 0,
    @field:JsonProperty("ip_lock_on")
    @SerialName("ip_lock_on")
    private val ipLockOn: Int = 0,
    @field:JsonProperty("dial_code")
    @SerialName("dial_code")
    private val dialCode: String = "",

    @field:JsonProperty("block_tor_ips")
    @SerialName("block_tor_ips")
    private val blockTorIps: Int = 0,
    @JsonProperty("currency")
    @SerialName("currency")
    val currency: String = "",

    @field:JsonProperty("notifications_confirmations")
    @SerialName("notifications_confirmations")
    private val notificationsConfirmations: Int = 0,
    @field:JsonProperty("auto_email_backup")
    @SerialName("auto_email_backup")
    private val autoEmailBackup: Int = 0,

    @field:JsonProperty("never_save_auth_type")
    @SerialName("never_save_auth_type")
    private val neverSaveAuthType: Int = 0,

    @JsonProperty("email")
    @SerialName("email")
    val email: String = "",

    @field:JsonProperty("sms_number")
    @SerialName("sms_number")
    private val _smsNumber: String? = null,
    @field:JsonProperty("sms_dial_code")
    @SerialName("sms_dial_code")
    val smsDialCode: String = "",

    @field:JsonProperty("sms_verified")
    @SerialName("sms_verified")
    private val smsVerified: Int = 0,

    @field:JsonProperty("is_api_access_enabled")
    @SerialName("is_api_access_enabled")
    private val isApiAccessEnabled: Int = 0,

    @field:JsonProperty("auth_type")
    @SerialName("auth_type")
    val authType: Int = 0,

    @field:JsonProperty("my_ip")
    @SerialName("my_ip")
    private val myIp: String = "",

    @field:JsonProperty("email_verified")
    @SerialName("email_verified")
    private val emailVerified: Int = 0,

    @field:JsonProperty("password_hint1")
    @SerialName("password_hint1")
    private val passwordHint1: String = "",

    @field:JsonProperty("country_code")
    @SerialName("country_code")
    val countryCode: String = "",

    @JsonProperty("state")
    @SerialName("state")
    private val state: String = "",

    @field:JsonProperty("logging_level")
    @SerialName("logging_level")
    private val loggingLevel: Int = 0,

    @JsonProperty("guid")
    @SerialName("guid")
    val guid: String = "",

    @JsonProperty("invited")
    @SerialName("invited")
    private val invited: HashMap<String, Boolean> = HashMap()

) {

    val isEmailVerified: Boolean
        get() = emailVerified.toBoolean()

    val isSmsVerified: Boolean
        get() = smsVerified.toBoolean()

    val isNotificationsOn: Boolean
        get() = notificationsOn.toBoolean()

    val isBlockTorIps: Boolean
        get() = blockTorIps.toBoolean()

    private fun Int.toBoolean(): Boolean {
        return this != 0
    }

    val smsNumber: String
        get() = _smsNumber ?: ""

    companion object {
        @JsonIgnore
        const val NOTIFICATION_ON = 2

        @JsonIgnore
        const val NOTIFICATION_OFF = 0

        @JsonIgnore
        const val NOTIFICATION_TYPE_NONE = 0

        @JsonIgnore
        const val NOTIFICATION_TYPE_EMAIL = 1

        @JsonIgnore
        const val NOTIFICATION_TYPE_SMS = 32

        @JsonIgnore
        const val NOTIFICATION_TYPE_ALL = 33

        @JsonIgnore
        const val AUTH_TYPE_OFF = 0

        @JsonIgnore
        const val AUTH_TYPE_YUBI_KEY = 1

        @JsonIgnore
        const val AUTH_TYPE_EMAIL = 2

        @JsonIgnore
        const val AUTH_TYPE_GOOGLE_AUTHENTICATOR = 4

        @JsonIgnore
        const val AUTH_TYPE_SMS = 5

        @JsonIgnore
        const val UNIT_BTC = "BTC"

        @JsonIgnore
        const val UNIT_MBC = "MBC"

        @JsonIgnore
        const val UNIT_UBC = "UBC"

     /*   @JsonIgnore
        val UNIT_FIAT = arrayOf(
            "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "DKK", "EUR", "GBP", "HKD",
            "ISK", "JPY", "KRW", "NZD", "PLN", "RUB", "SEK", "SGD", "THB", "TWD", "USD"
        )*/
    }
}
