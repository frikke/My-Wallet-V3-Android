package piuk.blockchain.android.ui.login

import android.net.Uri
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.network.PollResult
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.json.JSONObject
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo
import retrofit2.HttpException
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException

class LoginModel constructor(
    initialState: LoginState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val interactor: LoginInteractor,
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase,
    private val appMaintenanceFF: FeatureFlag,
    private val analytics: Analytics
) : MviModel<LoginState, LoginIntents>(initialState, mainScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: LoginState, intent: LoginIntents): Disposable? {
        return when (intent) {
            is LoginIntents.CheckAppMaintenanceStatus -> {
                checkAppMaintenanceStatus(intent.action, intent.uri)
                null
            }
            is LoginIntents.LoginWithQr -> loginWithQrCode(intent.qrString)
            is LoginIntents.ObtainSessionIdForEmail ->
                obtainSessionId(
                    intent.selectedEmail,
                    intent.captcha
                )
            is LoginIntents.SendEmail ->
                sendVerificationEmail(
                    intent.sessionId,
                    intent.selectedEmail,
                    intent.captcha,
                    previousState.pollingState
                )
            is LoginIntents.CheckForExistingSessionOrDeepLink -> {
                process(interactor.checkSessionDetails(intent.action, intent.uri))
                null
            }
            is LoginIntents.RevertToEmailInput -> {
                interactor.cancelPolling()
                null
            }
            is LoginIntents.CancelPolling -> {
                interactor.cancelPolling()
                null
            }
            is LoginIntents.ApproveLoginRequest -> handleApprovalStatusUpdate(
                isLoginApproved = true,
                sessionId = previousState.payload?.accountWallet?.sessionId.orEmpty(),
                base64Payload = previousState.payloadBase64String
            )
            is LoginIntents.DenyLoginRequest -> handleApprovalStatusUpdate(
                isLoginApproved = false,
                sessionId = previousState.payload?.accountWallet?.sessionId.orEmpty(),
                base64Payload = previousState.payloadBase64String
            )
            is LoginIntents.ResumePolling -> resumePollingIfNecessary(previousState)
            is LoginIntents.StartAuthPolling -> handlePayloadPollingResponse(previousState)
            is LoginIntents.CheckShouldNavigateToOtherScreen -> {
                if (interactor.shouldContinueToPinEntry()) {
                    process(LoginIntents.StartPinEntry)
                }
                null
            }
            else -> null
        }
    }

    private fun checkAppMaintenanceStatus(action: String?, uri: Uri?) {
        appMaintenanceFF.enabled.subscribe { enabled ->
            if (enabled) {
                rxSingle { getAppMaintenanceConfigUseCase() }.subscribe { status ->
                    when (status) {
                        AppMaintenanceStatus.NonActionable.Unknown,
                        AppMaintenanceStatus.NonActionable.AllClear -> {
                            checkExistingSessionOrDeepLink(action, uri)
                        }

                        else -> {
                            process(LoginIntents.ShowAppMaintenance)
                        }
                    }
                }
            } else {
                checkExistingSessionOrDeepLink(action, uri)
            }
        }
    }

    private fun checkExistingSessionOrDeepLink(action: String?, uri: Uri?) {
        if (action != null && uri != null) {
            process(LoginIntents.CheckForExistingSessionOrDeepLink(action, uri))
        }
    }

    private fun resumePollingIfNecessary(previousState: LoginState): Disposable? {
        Timber.d(previousState.toString())
        return if (previousState.pollingState == AuthPollingState.SUSPENDED) {
            handlePayloadPollingResponse(previousState)
        } else {
            null
        }
    }

    private fun handleApprovalStatusUpdate(
        isLoginApproved: Boolean,
        sessionId: String,
        base64Payload: String
    ): Disposable =
        interactor.updateApprovalStatus(
            isLoginApproved = isLoginApproved,
            sessionId = sessionId,
            base64Payload = base64Payload
        ).subscribeBy(
            onComplete = {
                // TODO for AND-5317 here we will look at the returned values from the API instead of receiving a completable
                proceedToNextScreen(isLoginApproved)
            },
            onError = {
                // TODO for AND-5317 here we will look at the returned values from the API instead of receiving a completable
                if (it is HttpException && it.code() == HttpsURLConnection.HTTP_CONFLICT) {
                    proceedToNextScreen(isLoginApproved)
                } else {
                    process(LoginIntents.UnknownError)
                }
            }
        )

    private fun proceedToNextScreen(isLoginApproved: Boolean) =
        if (interactor.shouldContinueToPinEntry()) {
            process(LoginIntents.StartPinEntryWithLoggingInPrompt(isLoginApproved = isLoginApproved))
        } else {
            process(LoginIntents.RevertToLauncher(isLoginApproved = isLoginApproved))
        }

    private fun handlePayloadPollingResponse(previousState: LoginState): Disposable {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        return Single.defer { interactor.pollForAuth(previousState.sessionId, jsonBuilder) }
            .subscribeBy(
                onSuccess = {
                    when (it) {
                        is PollResult.Cancel -> {
                            // do nothing - not a use-case here
                        }
                        is PollResult.TimeOut -> {
                            process(LoginIntents.AuthPollingTimeout)
                        }
                        is PollResult.FinalResult -> decodePayloadAndProceed(it.value, jsonBuilder)
                    }
                },
                onError = {
                    process(LoginIntents.AuthPollingError)
                }
            )
    }

    private fun decodePayloadAndProceed(body: ResponseBody, jsonBuilder: Json) {
        val bodyString = body.string()

        // first we try to decode the body as the full payload
        try {
            val requestData = jsonBuilder.decodeFromString<LoginAuthInfo.ExtendedAccountInfo>(bodyString)
            process(LoginIntents.PollingPayloadReceived(requestData))
        } catch (throwable: Throwable) {
            try {
                // if initial decoding fails, we try to decode as a denied payload
                val requestData = jsonBuilder.decodeFromString<LoginAuthInfo.PollingDeniedAccountInfo>(bodyString)

                // check if value is actually denied, otherwise, we have an unexpected error
                if (requestData.denied) {
                    process(LoginIntents.AuthPollingDenied)
                } else {
                    process(LoginIntents.AuthPollingError)
                }
            } catch (throwable: Throwable) {
                // if second decode fails, we have an unexpected error
                process(LoginIntents.AuthPollingError)
            }
        }
    }

    private fun loginWithQrCode(qrString: String): Disposable =
        interactor.loginWithQrCode(qrString)
            .subscribeBy(
                onComplete = {
                    process(LoginIntents.StartPinEntry)
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(
                        LoginIntents.ShowScanError(
                            shouldRestartApp = throwable is SSLPeerUnverifiedException
                        )
                    )
                }
            )

    private fun obtainSessionId(email: String, captcha: String): Disposable =
        interactor.obtainSessionId(email)
            .subscribeBy(
                onSuccess = { responseBody ->
                    val response = JSONObject(responseBody.string())
                    if (response.has(SESSION_TOKEN)) {
                        val sessionId = response.getString(SESSION_TOKEN)
                        process(LoginIntents.SendEmail(sessionId, email, captcha))
                    } else {
                        process(LoginIntents.GetSessionIdFailed)
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(LoginIntents.GetSessionIdFailed)
                }
            )

    private fun sendVerificationEmail(
        sessionId: String,
        email: String,
        captcha: String,
        pollingState: AuthPollingState
    ): Disposable =
        interactor.sendEmailForVerification(sessionId, email, captcha)
            .subscribeBy(
                onComplete = {
                    process(LoginIntents.ShowEmailSent)
                    if (pollingState == AuthPollingState.NOT_STARTED) {
                        process(LoginIntents.StartAuthPolling)
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    if (throwable is HttpException) {
                        analytics.logEvent(
                            LoginAnalytics.LoginEmailFailed(
                                httpErrorCode = throwable.code(),
                                errorMessage = throwable.message()
                            )
                        )
                    } else {
                        analytics.logEvent(
                            LoginAnalytics.LoginEmailFailed(
                                httpErrorCode = null,
                                errorMessage = throwable.message.toString()
                            )
                        )
                    }
                    process(LoginIntents.ShowEmailFailed)
                }
            )

    companion object {
        private const val SESSION_TOKEN = "token"
    }
}
