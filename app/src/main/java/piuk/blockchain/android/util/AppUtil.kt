package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import com.blockchain.logging.DigitalTrust
import info.blockchain.wallet.payload.PayloadManagerWiper
import piuk.blockchain.android.ui.auth.LogoutActivity
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.isValidGuid

class AppUtil(
    private val context: Context,
    private var payloadManager: PayloadManagerWiper,
    private val prefs: PersistentPrefs,
    private val trust: DigitalTrust,
    private val pinRepository: PinRepository
) {
    fun logout() {
        pinRepository.clearPin()
        trust.clearUserId()
        context.startActivity(
            Intent(context, LogoutActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                action = BlockchainActivity.LOGOUT_ACTION
            }
        )
    }

    fun unpairWallet() {
        pinRepository.clearPin()
        prefs.unPairWallet()
    }

    val isSane: Boolean
        get() {
            val guid = prefs.walletGuid
            val encryptedPassword = prefs.encryptedPassword
            val pinID = prefs.pinId

            return guid.isValidGuid() && encryptedPassword.isNotEmpty() && pinID.isNotEmpty()
        }

    var activityIndicator: ActivityIndicator? = null

    fun clearCredentials() {
        payloadManager.wipe()
        prefs.clear()
    }

    fun clearCredentialsAndRestart() {
        clearCredentials()
        restartApp()
    }

    fun restartApp() {
        context.startActivity(
            Intent(context, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun restartAppWithVerifiedPin(launcherActivity: Class<*>, isAfterWalletCreation: Boolean = false) {
        context.startActivity(
            Intent(context, launcherActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(INTENT_EXTRA_VERIFIED, true)
                putExtra(INTENT_EXTRA_IS_AFTER_WALLET_CREATION, isAfterWalletCreation)
            }
        )

        prefs.isAppUnlocked = false
    }

    companion object {
        const val INTENT_EXTRA_VERIFIED = "verified"
        const val INTENT_EXTRA_IS_AFTER_WALLET_CREATION = "is_after_wallet_creation"
    }
}
