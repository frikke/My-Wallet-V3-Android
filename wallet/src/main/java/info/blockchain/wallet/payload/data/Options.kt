package info.blockchain.wallet.payload.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Options(
    @SerialName("pbkdf2_iterations")
    var pbkdf2Iterations: Int = 0,

    @SerialName("fee_per_kb")
    val feePerKb: Long = 0,

    @SerialName("html5_notifications")
    val isHtml5Notifications: Boolean = false,

    @SerialName("logout_time")
    val logoutTime: Long = 0
) {

    companion object {
        private const val DEFAULT_FEE_PER_KB = 10000L
        private const val DEFAULT_LOGOUT_TIME = 600000L
        private const val DEFAULT_HTML5_NOTIFICATIONS = false

        val defaultOptions: Options
            get() {
                return Options(
                    pbkdf2Iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2,
                    isHtml5Notifications = DEFAULT_HTML5_NOTIFICATIONS,
                    logoutTime = DEFAULT_LOGOUT_TIME,
                    feePerKb = DEFAULT_FEE_PER_KB
                )
            }
    }
}
