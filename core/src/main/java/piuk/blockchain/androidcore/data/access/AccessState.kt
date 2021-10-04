package piuk.blockchain.androidcore.data.access

import android.content.Context
import android.content.Intent
import com.blockchain.logging.CrashLogger
import com.blockchain.logging.DigitalTrust
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.isValidPin

interface AccessState {

    val pin: String

    fun setLogoutActivity(logoutActivity: Class<*>)

    fun logout()

    fun unpairWallet()

    fun clearPin()
    fun setPin(pin: String)

    companion object {
        const val LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT"
    }
}

internal class AccessStateImpl(
    val context: Context,
    val prefs: PersistentPrefs,
    private val trust: DigitalTrust,
    private val crashLogger: CrashLogger
) : AccessState {

    private var logoutActivity: Class<*>? = null

    private var thePin: String = ""
    override val pin: String
        get() = thePin

    override fun clearPin() {
        thePin = ""
    }

    override fun setPin(pin: String) {
        if (!pin.isValidPin()) {
            IllegalArgumentException("setting invalid pin!").let {
                crashLogger.logException(it)
                throw it
            }
        }
        thePin = pin
    }

    override fun setLogoutActivity(logoutActivity: Class<*>) {
        this.logoutActivity = logoutActivity
    }

    override fun logout() {
        crashLogger.logEvent("logout. resetting pin")
        clearPin()
        trust.clearUserId()
        context.startActivity(
            Intent(context, logoutActivity!!).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                action = AccessState.LOGOUT_ACTION
            }
        )
    }

    override fun unpairWallet() {
        crashLogger.logEvent("unpair. resetting pin")
        clearPin()
        prefs.unPairWallet()
    }
}