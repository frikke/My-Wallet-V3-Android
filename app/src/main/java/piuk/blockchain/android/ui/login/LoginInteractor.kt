package piuk.blockchain.android.ui.login

import android.net.Uri
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import piuk.blockchain.android.ui.login.auth.LoginAuthActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class LoginInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val ssoAccountRecoveryFF: FeatureFlag,
    private val persistentPrefs: PersistentPrefs
) {

    fun loginWithQrCode(qrString: String): Completable =
        payloadDataManager.handleQrCode(qrString)
            .doOnComplete {
                payloadDataManager.wallet?.let { wallet ->
                    prefs.apply {
                        sharedKey = wallet.sharedKey
                        walletGuid = wallet.guid
                        emailVerified = true
                        isOnBoardingComplete = true
                    }
                }
            }
            .doOnError { appUtil.clearCredentials() }

    fun obtainSessionId(email: String): Single<ResponseBody> =
        authDataManager.createSessionId(email)

    fun sendEmailForVerification(
        sessionId: String,
        email: String,
        captcha: String
    ): Completable {
        prefs.sessionId = sessionId
        return ssoAccountRecoveryFF.enabled.flatMapCompletable { enabled ->
            if (enabled) {
                authDataManager.sendEmailForAuthentication(sessionId, email, captcha)
            } else {
                authDataManager.sendEmailForDeviceVerification(sessionId, email, captcha)
                    .ignoreElement()
            }
        }
    }

    fun checkSessionDetails(intentAction: String, uri: Uri): LoginIntents =
        when {
            persistentPrefs.pinId.isNotEmpty() -> {
                LoginIntents.UserIsLoggedIn
            }
            uri.hasDeeplinkData() -> LoginIntents.UserAuthenticationRequired(intentAction, uri)
            else -> LoginIntents.UnknownError
        }

    private fun Uri.hasDeeplinkData() =
        fragment?.let { data -> data.split(LoginAuthActivity.LINK_DELIMITER).size > 1 }
            ?: false
}