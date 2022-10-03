package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Options(
    /**
     * Missing in <v3
     */
    @SerialName("pbkdf2_iterations")
    val pbkdf2Iterations: Int? = null,
    /**
     * Missing in <v3
     */
    @SerialName("fee_per_kb")
    val feePerKb: Long? = null,

    @SerialName("html5_notifications")
    private val _isHtml5Notifications: Boolean? = null,

    @SerialName("logout_time")
    private val _logoutTime: Long? = null
) {

    val logoutTime: Long
        get() = _logoutTime ?: DEFAULT_LOGOUT_TIME

    companion object {
        private const val DEFAULT_FEE_PER_KB = 10000L
        private const val DEFAULT_LOGOUT_TIME = 600000L
        private const val DEFAULT_HTML5_NOTIFICATIONS = false

        val defaultOptions: Options
            get() {
                return Options(
                    pbkdf2Iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2,
                    _isHtml5Notifications = DEFAULT_HTML5_NOTIFICATIONS,
                    _logoutTime = DEFAULT_LOGOUT_TIME,
                    feePerKb = DEFAULT_FEE_PER_KB
                )
            }
    }
}
