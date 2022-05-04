package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.AppUtilAPI
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.DigitalTrust
import com.blockchain.logging.RemoteLogger
import info.blockchain.wallet.payload.PayloadScopeWiper
import io.intercom.android.sdk.Intercom
import piuk.blockchain.android.ui.auth.LogoutActivity
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.utils.PersistentPrefs

class AppUtil(
    private val context: Context,
    private var payloadScopeWiper: PayloadScopeWiper,
    private val prefs: PersistentPrefs,
    private val trust: DigitalTrust,
    private val pinRepository: PinRepository,
    private val remoteLogger: RemoteLogger,
    private val isIntercomEnabledFlag: FeatureFlag
) : AppUtilAPI {
    override fun logout() {
        pinRepository.clearPin()
        trust.clearUserId()
        context.startActivity(
            Intent(context, LogoutActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                action = BlockchainActivity.LOGOUT_ACTION
            }
        )
        if (isIntercomEnabledFlag.isEnabled) {
            Intercom.client().logout()
        }
    }

    fun unpairWallet() {
        pinRepository.clearPin()
        prefs.unPairWallet()
    }

    override var activityIndicator: ActivityIndicator? = null

    fun clearCredentials() {
        remoteLogger.logEvent("Clearing credentials")
        payloadScopeWiper.wipe()
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

    fun loadAppWithVerifiedPin(loaderActivity: Class<*>, isAfterWalletCreation: Boolean = false) {
        context.startActivity(
            Intent(context, loaderActivity).apply {
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
