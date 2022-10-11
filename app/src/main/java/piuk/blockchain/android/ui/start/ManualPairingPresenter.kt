package piuk.blockchain.android.ui.start

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import piuk.blockchain.android.R
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

interface ManualPairingView : PasswordAuthView

class ManualPairingPresenter(
    override val appUtil: AppUtil,
    override val authDataManager: AuthDataManager,
    override val payloadDataManager: PayloadDataManager,
    override val authPrefs: AuthPrefs,
    private val analytics: Analytics,
    override val remoteLogger: RemoteLogger
) : PasswordAuthPresenter<ManualPairingView>() {

    internal fun onContinueClicked(guid: String, password: String) {
        when {
            guid.isEmpty() -> showErrorSnackbar(R.string.invalid_guid)
            password.isEmpty() -> showErrorSnackbar(R.string.invalid_password)
            else -> verifyPassword(password, guid)
        }
    }

    override fun onAuthFailed() {
        super.onAuthFailed()
        showErrorSnackbar(R.string.auth_failed)
    }

    override fun onAuthComplete() {
        super.onAuthComplete()
        analytics.logEvent(AnalyticsEvents.WalletManualLogin)
    }
}
