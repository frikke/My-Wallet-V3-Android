package piuk.blockchain.android.ui.start

import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.logging.CrashLogger
import piuk.blockchain.android.R
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

interface PasswordRequiredView : PasswordAuthView {
    fun restartPage()
    fun showForgetWalletWarning()
    fun showWalletGuid(guid: String)
}

class PasswordRequiredPresenter(
    override val appUtil: AppUtil,
    override val prefs: PersistentPrefs,
    override val authDataManager: AuthDataManager,
    override val payloadDataManager: PayloadDataManager,
    override val crashLogger: CrashLogger
) : PasswordAuthPresenter<PasswordRequiredView>() {

    fun onContinueClicked(password: String) {
        if (password.length > 1) {
            val guid = prefs.walletGuid
            verifyPassword(password, guid)
        } else {
            view?.apply {
                showSnackbar(R.string.invalid_password, SnackbarType.Error)
                restartPage()
            }
        }
    }

    fun onForgetWalletClicked() {
        view?.showForgetWalletWarning()
    }

    fun loadWalletGuid() {
        view?.showWalletGuid(prefs.walletGuid)
    }

    fun onForgetWalletConfirmed() {
        appUtil.clearCredentialsAndRestart()
    }

    override fun onAuthFailed() {
        super.onAuthFailed()
        showErrorSnackbarAndRestartApp(R.string.auth_failed)
    }
}
