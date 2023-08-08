package piuk.blockchain.android.ui.start

import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import piuk.blockchain.android.R
import piuk.blockchain.android.util.AppUtil

interface PasswordRequiredView : PasswordAuthView {
    fun restartPage()
    fun showForgetWalletWarning()
    fun showWalletGuid(guid: String)
}

class PasswordRequiredPresenter(
    override val appUtil: AppUtil,
    override val authPrefs: AuthPrefs,
    override val authDataManager: AuthDataManager,
    override val payloadDataManager: PayloadDataManager,
    override val remoteLogger: RemoteLogger
) : PasswordAuthPresenter<PasswordRequiredView>() {

    fun onContinueClicked(password: String) {
        if (password.length > 1) {
            val guid = authPrefs.walletGuid
            verifyPassword(password, guid)
        } else {
            view?.apply {
                showSnackbar(com.blockchain.stringResources.R.string.invalid_password, SnackbarType.Error)
                restartPage()
            }
        }
    }

    fun checkEmailAuth(password: String) {
        if (password.isNotEmpty() && hasTimerStarted()) {
            val guid = authPrefs.walletGuid
            waitForEmailAuth(password, guid)
        }
    }

    fun onForgetWalletClicked() {
        view?.showForgetWalletWarning()
    }

    fun loadWalletGuid() {
        view?.showWalletGuid(authPrefs.walletGuid)
    }

    fun onForgetWalletConfirmed(
        redirectLandingToLogin: Boolean = false
    ) {
        appUtil.clearCredentialsAndRestart(redirectLandingToLogin = redirectLandingToLogin)
    }

    override fun onAuthFailed() {
        super.onAuthFailed()
        showErrorSnackbarAndRestartApp(com.blockchain.stringResources.R.string.auth_failed)
    }
}
