package piuk.blockchain.android.ui.login

import android.net.Uri
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
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.SessionPrefs
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
                if (uri.hasDeeplinkData()) {
                    decodePayloadAndNavigate(uri, builder, intentAction)
                } else {
                    LoginIntents.UserLoggedInWithoutDeeplinkData
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

    companion object {
        private const val POLLING_INTERVAL = 2L
        private const val POLLING_RETRIES = 50
        private const val EMPTY_BODY = "{}"
    }
}
