package piuk.blockchain.android.ui.login

import android.net.Uri
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.auth.isValidGuid
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.network.PollResult
import com.blockchain.network.PollService
import com.blockchain.preferences.AuthPrefs
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.nio.charset.Charset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import piuk.blockchain.android.ui.login.auth.LoginAuthActivity
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo
import piuk.blockchain.android.util.AppUtil
import timber.log.Timber

class LoginInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val authPrefs: AuthPrefs,
    private val appUtil: AppUtil
) {
    private lateinit var authPollService: PollService<ResponseBody>

    fun loginWithQrCode(qrString: String): Completable =
        payloadDataManager.handleQrCode(qrString)
            .doOnComplete {
                payloadDataManager.wallet?.let { wallet ->
                    authPrefs.apply {
                        sharedKey = wallet.sharedKey
                        walletGuid = wallet.guid
                        emailVerified = true
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
        authPrefs.sessionId = sessionId
        return authDataManager.sendEmailForAuthentication(sessionId, email, captcha)
    }

    @ExperimentalSerializationApi
    fun checkSessionDetails(intentAction: String, uri: Uri): LoginIntents {
        val builder = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }

        return when {
            authPrefs.pinId.isNotEmpty() -> {
                when {
                    uri.hasDeeplinkData() -> decodePayloadAndNavigate(uri, builder, intentAction)
                    uri.isWalletConnectDeeplink() -> decodeWCLinkAndNavigate(uri)
                    else -> LoginIntents.UserLoggedInWithoutDeeplinkData
                }
            }
            uri.hasDeeplinkData() -> decodePayloadAndNavigate(uri, builder, intentAction)
            else -> LoginIntents.UnknownError
        }
    }

    @ExperimentalSerializationApi
    private fun decodePayloadAndNavigate(uri: Uri, builder: Json, intentAction: String): LoginIntents =
        try {
            PayloadHandler.getDataFromUri(uri)?.let { data ->
                val sessionId = authPrefs.sessionId
                if (data.isValidGuid()) return@let LoginIntents.ShowManualPairing(data)
                val decodedJson = PayloadHandler.decodeToJsonString(data)
                val accountInfo = builder.decodeFromString<LoginAuthInfo.ExtendedAccountInfo>(decodedJson)
                if (sessionId.isEmpty() || accountInfo.accountWallet.sessionId != sessionId) {
                    LoginIntents.ReceivedExternalLoginApprovalRequest(data, accountInfo)
                } else {
                    LoginIntents.UserAuthenticationRequired(intentAction, uri)
                }
            } ?: LoginIntents.UnknownError
        } catch (e: Throwable) {
            LoginIntents.UnknownError
        }

    @ExperimentalSerializationApi
    private fun decodeWCLinkAndNavigate(uri: Uri): LoginIntents {
        // Example: https://login.blockchain.com/deeplink/login/wallet-connect/wc?uri=
        // wc:00e46b69-d0cc-4b3e-b6a2-cee442f97188@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=
        // 91303dedf64285cbbaf9120f6e9d160a5c8aa3deb67017a3874cd272323f48ae
        val wcUri = uri.getQueryParameter(PARAMETER_URI)
        val wcKey = uri.getQueryParameter(PARAMETER_KEY)
        // We need everything after the uri bit but querying the uri doesn't return the key part
        return LoginIntents.WalletConnectDeeplinkReceived("$wcUri&$PARAMETER_KEY=$wcKey")
    }

    fun shouldContinueToPinEntry() = authPrefs.pinId.isNotEmpty()

    fun updateApprovalStatus(isLoginApproved: Boolean, sessionId: String, base64Payload: String): Completable =
        authDataManager.updateLoginApprovalStatus(sessionId, base64Payload, isLoginApproved)

    fun cancelPolling() {
        if (::authPollService.isInitialized) {
            authPollService.cancel.onNext(true)
        }
    }

    fun pollForAuth(sessionId: String, json: Json): Single<PollResult<ResponseBody>> {
        authPollService = PollService(
            authDataManager.getDeeplinkPayload(sessionId)
        ) {
            val responseBodyString = it.peekResponseBody()
            try {
                val pollingInfo = json.decodeFromString<LoginAuthInfo.ContinuePollingForInfo>(responseBodyString)
                pollingInfo.responseType != LoginAuthInfo.ContinuePollingForInfo.CONTINUE_POLLING
            } catch (t: Throwable) {
                Timber.e("Poll for wallet info - failed to parse, stop polling: $t")
                false
            }
        }

        return authPollService.start(POLLING_INTERVAL, POLLING_RETRIES)
    }

    private fun ResponseBody.peekResponseBody(): String {
        val bodySource = this.source()
        bodySource.request(Long.MAX_VALUE)
        val buffer = bodySource.buffer
        return buffer.clone().readString(Charset.forName("UTF-8"))
    }

    private fun Uri.hasDeeplinkData() =
        fragment?.let { data -> data.split(LoginAuthActivity.LINK_DELIMITER).size > 1 }
            ?: false

    private fun Uri.isWalletConnectDeeplink() =
        toString().contains(WALLETCONNECT_URL)

    companion object {
        private const val POLLING_INTERVAL = 2L
        private const val POLLING_RETRIES = 50
        private const val PARAMETER_URI = "uri"
        private const val PARAMETER_KEY = "key"
        private const val WALLETCONNECT_URL = "/login/wallet-connect/wc"
    }
}
