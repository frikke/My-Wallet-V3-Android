package info.blockchain.wallet.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Settings(

    @SerialName("btc_currency")
    private val btcCurrency: String = "",
    @SerialName("notifications_type")
    val notificationsType: List<Int> = emptyList(),
    @SerialName("language")
    private val language: String = "",
    @SerialName("notifications_on")
    private val notificationsOn: Int = 0,
    @SerialName("ip_lock_on")
    private val ipLockOn: Int = 0,
    @SerialName("dial_code")
    private val dialCode: String = "",

    @SerialName("block_tor_ips")
    private val blockTorIps: Int = 0,
    @SerialName("currency")
    val currency: String = "",

    @SerialName("notifications_confirmations")
    private val notificationsConfirmations: Int = 0,
    @SerialName("auto_email_backup")
    private val autoEmailBackup: Int = 0,

    @SerialName("never_save_auth_type")
    private val neverSaveAuthType: Int = 0,

    @SerialName("email")
    val email: String = "",

    @SerialName("sms_number")
    private val _smsNumber: String? = null,
    @SerialName("sms_dial_code")
    val smsDialCode: String = "",

    @SerialName("sms_verified")
    private val smsVerified: Int = 0,

    @SerialName("is_api_access_enabled")
    private val isApiAccessEnabled: Int = 0,

    @SerialName("auth_type")
    val authType: Int = 0,

    @SerialName("my_ip")
    private val myIp: String = "",

    @SerialName("email_verified")
    private val emailVerified: Int = 0,

    @SerialName("password_hint1")
    private val passwordHint1: String = "",

    @SerialName("country_code")
    val countryCode: String = "",

    @SerialName("state")
    private val state: String = "",

    @SerialName("logging_level")
    private val loggingLevel: Int = 0,

    @SerialName("guid")
    val guid: String = "",

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
        get() = _smsNumber.orEmpty()

    companion object {
        const val NOTIFICATION_ON = 2

        const val NOTIFICATION_OFF = 0

        const val NOTIFICATION_TYPE_NONE = 0

        const val NOTIFICATION_TYPE_EMAIL = 1

        const val NOTIFICATION_TYPE_SMS = 32

        const val NOTIFICATION_TYPE_ALL = 33

        const val AUTH_TYPE_OFF = 0

        const val AUTH_TYPE_YUBI_KEY = 1

        const val AUTH_TYPE_EMAIL = 2

        const val AUTH_TYPE_GOOGLE_AUTHENTICATOR = 4

        const val AUTH_TYPE_SMS = 5

        const val UNIT_BTC = "BTC"

        const val UNIT_MBC = "MBC"

        const val UNIT_UBC = "UBC"
    }
}
